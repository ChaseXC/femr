package femr.ui.controllers;

import com.google.inject.Inject;
import femr.business.dtos.CurrentUser;
import femr.business.dtos.ServiceResponse;
import femr.business.services.ISessionService;
import femr.business.services.ISuperuserService;
import femr.common.models.Roles;
import femr.common.models.custom.ICustomTab;
import femr.data.models.custom.CustomTab;
import femr.ui.helpers.security.AllowedRoles;
import femr.ui.helpers.security.FEMRAuthenticated;
import femr.ui.models.data.CustomFieldItem;
import femr.ui.models.superuser.*;
import femr.util.stringhelpers.StringUtils;
import org.joda.time.DateTime;
import play.api.libs.iteratee.internal;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import femr.ui.views.html.superuser.*;

import java.util.ArrayList;
import java.util.List;

@Security.Authenticated(FEMRAuthenticated.class)
@AllowedRoles({Roles.SUPERUSER})
public class SuperuserController extends Controller {
    private final Form<TabsViewModelPost> TabsViewModelForm = Form.form(TabsViewModelPost.class);
    private final Form<ContentViewModelPost> ContentViewModelForm = Form.form(ContentViewModelPost.class);
    private ISessionService sessionService;
    private ISuperuserService superuserService;

    @Inject
    public SuperuserController(ISessionService sessionService,
                               ISuperuserService superuserService){
        this.sessionService = sessionService;
        this.superuserService = superuserService;
    }

    public Result indexGet(){

        CurrentUser currentUser = sessionService.getCurrentUserSession();
        return ok(index.render(currentUser));
    }

    public Result tabsGet(){
        CurrentUser currentUser = sessionService.getCurrentUserSession();

        ServiceResponse<List<? extends ICustomTab>> response;
        response = superuserService.getCustomMedicalTabs(false);
        if (response.hasErrors()){
            return internalServerError();
        }

        TabsViewModelGet viewModelGet = new TabsViewModelGet();
        List<String> currentTabNames = new ArrayList<>();
        for (ICustomTab ct : response.getResponseObject()){
            currentTabNames.add(ct.getName());
        }
        viewModelGet.setCurrentTabNames(currentTabNames);

        //get deleted tabs
        response = superuserService.getCustomMedicalTabs(true);
        List<String> deletedTabNames = new ArrayList<>();
        for (ICustomTab ct : response.getResponseObject()){
            deletedTabNames.add(ct.getName());
        }
        viewModelGet.setDeletedTabNames(deletedTabNames);




        return ok(tabs.render(currentUser, viewModelGet));
    }

    public Result tabsPost(){
        CurrentUser currentUser = sessionService.getCurrentUserSession();
        TabsViewModelPost viewModelPost = TabsViewModelForm.bindFromRequest().get();
        if (StringUtils.isNotNullOrWhiteSpace(viewModelPost.getNewTab())){
            ICustomTab customTab = new CustomTab();
            customTab.setDateCreated(DateTime.now());
            customTab.setName(viewModelPost.getNewTab());
            customTab.setUserId(currentUser.getId());
            customTab.setIsDeleted(false);
            ServiceResponse<ICustomTab> response = superuserService.createCustomMedicalTab(customTab);
            if (response.hasErrors()){
                return internalServerError();
            }
        }
        if (StringUtils.isNotNullOrWhiteSpace(viewModelPost.getDeleteTab())){
            ServiceResponse<ICustomTab> response = superuserService.removeCustomMedicalTab(viewModelPost.getDeleteTab());
            if (response.hasErrors()){
                return internalServerError();
            }
        }

        return redirect("/superuser/tabs");
    }

    //name = tab name
    public Result contentGet(String name){
        CurrentUser currentUser = sessionService.getCurrentUserSession();

        ContentViewModelGet viewModelGet = new ContentViewModelGet();
        viewModelGet.setName(name);

        ServiceResponse<List<CustomFieldItem>> currentFieldItemsResponse = superuserService.getCustomFields(false);
        if (currentFieldItemsResponse.hasErrors()){
            return internalServerError();
        }
        viewModelGet.setCurrentCustomFieldItemList(currentFieldItemsResponse.getResponseObject());

        ServiceResponse<List<CustomFieldItem>> removedFieldItemsResponse = superuserService.getCustomFields(true);
        if (currentFieldItemsResponse.hasErrors()){
            return internalServerError();
        }
        viewModelGet.setRemovedCustomFieldItemList(removedFieldItemsResponse.getResponseObject());

        return ok(content.render(currentUser, viewModelGet));

    }

    //name = tab name
    public Result contentPost(String name){
        CurrentUser currentUser = sessionService.getCurrentUserSession();

        ContentViewModelPost viewModelPost = ContentViewModelForm.bindFromRequest().get();

        //to do: determine if adding or removing shit
        CustomFieldItem customFieldItem = new CustomFieldItem();
        customFieldItem.setName(viewModelPost.getAddName().toLowerCase());
        customFieldItem.setSize(viewModelPost.getAddSize().toLowerCase());
        customFieldItem.setType(viewModelPost.getAddType().toLowerCase());

        ServiceResponse<CustomFieldItem> response = superuserService.createCustomField(customFieldItem, currentUser.getId(), name);
        if (response.hasErrors()){
            return internalServerError();
        }


        return redirect("/superuser/tabs/" + name);
    }

}
