package com.x.cms.assemble.control.jaxrs.appinfo;

import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.ListTools;
import com.x.base.core.project.tools.SortTools;
import net.sf.ehcache.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class ActionListWhatICanViewData_WithAppType extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionListWhatICanViewData_WithAppType.class);

	@SuppressWarnings("unchecked")
	protected ActionResult<List<Wo>> execute(HttpServletRequest request, EffectivePerson effectivePerson, String appType )
			throws Exception {
		ActionResult<List<Wo>> result = new ActionResult<>();
		List<Wo> wos = new ArrayList<>();
		List<Wo> wos_out = new ArrayList<>();
		Boolean isXAdmin = false;
		Boolean check = true;
		Boolean isAnonymous = effectivePerson.isAnonymous();
		String personName = effectivePerson.getDistinguishedName();
		
		try {
			isXAdmin = userManagerService.isManager( effectivePerson );
		} catch (Exception e) {
			check = false;
			Exception exception = new ExceptionAppInfoProcess(e, "系统在检查用户是否是平台管理员时发生异常。Name:" + personName);
			result.error(exception);
			logger.error(e, effectivePerson, request, null);
		}
		
		String cacheKey = ApplicationCache.concreteCacheKey(personName, "Data", appType, isXAdmin);
		Element element = cache.get(cacheKey);

		if ((null != element) && (null != element.getObjectValue())) {
			wos = (List<Wo>) element.getObjectValue();
			result.setData(wos);
		} else {
			if (check) {
				try {
					wos_out = listViewAbleAppInfoByPermission( personName, isAnonymous, null, appType, "数据", isXAdmin, 1000 );
				} catch (Exception e) {
					check = false;
					Exception exception = new ExceptionAppInfoProcess(e, "系统在根据用户权限查询所有可见的分类信息时发生异常。Name:" + personName);
					result.error(exception);
					logger.error(e, effectivePerson, request, null);
				}
				if( ListTools.isNotEmpty( wos_out )){
					for( Wo wo : wos_out ) {
						if( ListTools.isNotEmpty( wo.getWrapOutCategoryList() )) {

							try {
								wo.setConfig( appInfoServiceAdv.getConfigJson( wo.getId() ) );
							} catch (Exception e) {
								check = false;
								Exception exception = new ExceptionAppInfoProcess(e, "系统根据ID查询栏目配置支持信息时发生异常。ID=" + wo.getId() );
								result.error(exception);
								logger.error(e, effectivePerson, request, null);
							}

							wos.add( wo );
						}
					}
					//按appInfoSeq列的值， 排个序
					SortTools.asc( wos, "appInfoSeq");
					cache.put(new Element( cacheKey, wos ));
					result.setData( wos );
				}
			}
		}
		return result;
	}
}