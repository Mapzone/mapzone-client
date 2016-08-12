package io.mapzone.controller.ui.project;

import static java.util.stream.Collectors.toMap;
import static org.polymap.core.runtime.UIThreadExecutor.asyncFast;
import static org.polymap.rhei.batik.toolkit.md.dp.dp;
import io.mapzone.controller.ControllerPlugin;
import io.mapzone.controller.ops.CreateProjectOperation;
import io.mapzone.controller.ui.CtrlPanel;
import io.mapzone.controller.ui.DashboardPanel;
import io.mapzone.controller.ui.util.PropertyAdapter;
import io.mapzone.controller.um.repository.Organization;
import io.mapzone.controller.um.repository.Project;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.operation.OperationSupport;
import org.polymap.core.security.UserPrincipal;
import org.polymap.core.ui.ColumnLayoutFactory;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.PanelPath;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper.Quadrant;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.batik.toolkit.md.MdToolkit;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.PlainValuePropertyAdapter;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.rhei.form.batik.BatikFormContainer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class CreateProjectPanel
        extends CtrlPanel {

    private static Log log = LogFactory.getLog( CreateProjectPanel.class );

    public static final PanelIdentifier ID = PanelIdentifier.parse( "createProject" );

    private final Image             icon = ControllerPlugin.images().svgOverlayedImage( 
            "map.svg", ControllerPlugin.HEADER_ICON_CONFIG,
            "plus-circle-filled.svg", SvgImageRegistryHelper.OVR12_ACTION, 
            Quadrant.TopRight );

    @Scope("io.mapzone.controller")
    protected Context<UserPrincipal> userPrincipal;
    
    /**
     * The operation to be prepared and executed by this panel.
     */
    private CreateProjectOperation  op;

    private BatikFormContainer      projectForm;

    private BatikFormContainer      launcherForm;

    private Button                  fab;

    
    @Override
    public boolean wantsToBeShown() {
        if (parentPanel().get() instanceof DashboardPanel) {
            site().title.set( "" );
            site().icon.set( icon );
            return true;
        }
        return false;
    }


    @Override
    public void init() {
        super.init();
        site().title.set( "New project" );
        
        op = new CreateProjectOperation( userPrincipal.get().getName() );
        op.createProject();
    }


    @Override
    public void createContents( Composite parent ) {
        MdToolkit tk = (MdToolkit)getSite().toolkit();
        
//        parent.setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).spacing( 10 ).create() );
        
        // welcome
        IPanelSection welcomeSection = tk.createPanelSection( parent, "Set up a new project" );
        welcomeSection.addConstraint( new PriorityConstraint( 100 ), new MinWidthConstraint( 350, 1 ) );
        tk.createFlowText( welcomeSection.getBody(), "Choose an Organization your are member of. Or you choose to create a personal project. Personal projects can be asigned to an Organization later." );

        // Project
        IPanelSection projectSection = tk.createPanelSection( parent, "Project", SWT.BORDER );
        projectSection.addConstraint( new PriorityConstraint( 10 ) );
        projectSection.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).spacing( dp(10).pix() ).create() );
        
        projectForm = new BatikFormContainer( new ProjectForm() );
        projectForm.createContents( projectSection.getBody() );

//        // ProjectLauncher
//        IPanelSection launcherSection = tk.createPanelSection( parent, "Project launcher", SWT.BORDER );
//        launcherSection.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).spacing( dp(10).pix() ).create() );
//        
//        launcherForm = new BatikFormContainer( new ProjectForm() );
//        launcherForm.createContents( launcherSection.getBody() );

        // FAB
        fab = tk.createFab();
        fab.setToolTipText( "Create the new project" );
        fab.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                try {
                    projectForm.submit( null );
                }
                catch (Exception e) {
                    StatusDispatcher.handleError( "Unable to create project.", e );
                    return;
                }
                
                // XXX execute sync as long as there is no progress indicator
                OperationSupport.instance().execute2( op, false, false, ev2 -> asyncFast( () -> {
                    if (ev2.getResult().isOK()) {
                        PanelPath panelPath = getSite().getPath();
                        getContext().closePanel( panelPath );                        
                    }
                    else {
                        StatusDispatcher.handleError( "Unable to create project.", ev2.getResult().getException() );
                    }
                }));
            }
        } );
    }

    
    protected void updateEnabled() {
        fab.setVisible( projectForm.isDirty() && projectForm.isValid() );
    }
    
    
    /**
     * {@link Project} settings. 
     */
    class ProjectForm
            extends DefaultFormPage 
            implements IFormFieldListener {
        
        @Override
        public void createFormContents( IFormPageSite site ) {
            super.createFormContents( site );
            
            Composite body = site.getPageBody();
            body.setLayout( ColumnLayoutFactory.defaults()
                    .spacing( 5 /*panelSite.getLayoutPreference( LAYOUT_SPACING_KEY ) / 4*/ )
                    .margins( getSite().getLayoutPreference().getSpacing() / 2 ).create() );
            
            // organization
            Map<String,Organization> orgs = op.user.get().organizations.stream()
                    .map( role -> role.organization.get() )
                    .collect( toMap( org -> org.name.get(), org -> org ) );
            site.newFormField( new PlainValuePropertyAdapter( "organizationOrUser", null ) )
                    .label.put( "Organization or User" )
                    .field.put( new PicklistFormField( orgs ) )
                    .tooltip.put( "" )
                    .create();
            
            // name
            site.newFormField( new PropertyAdapter( op.project.get().name ) )
                    .validator.put( new NotEmptyValidator<String,String>() {
                        @Override
                        public String validate( String fieldValue ) {
                            String result = super.validate( fieldValue );
                            if (result == null) {
                                if (op.organization.isPresent()) {
                                    if (op.umUow.get().findProject( op.organization.get().name.get(), (String)fieldValue ).isPresent()) { 
                                        result = "Project name is already taken";
                                    }
                                }
                                else {
                                    result = "Choose an organization first";
                                }
                            };
                            return result;
                        }
                    }).create();
            
            // description
            site.newFormField( new PropertyAdapter( op.project.get().description ) ).create();
            
            // website
            site.newFormField( new PropertyAdapter( op.project.get().website ) ).create();
            
            // location
            site.newFormField( new PropertyAdapter( op.project.get().location ) ).create();
            
            site.addFieldListener( this );
        }

        
        @Override
        public void fieldChange( FormFieldEvent ev ) {
            if (ev.getEventCode() == VALUE_CHANGE) {
                if (ev.getFieldName().equals( "organizationOrUser" )) {
                    ev.getNewModelValue().ifPresent( v -> op.organization.set( (Organization)v ) );
                   // op.organization.set( (Organization)ev.getNewModelValue().orElse( null ) );
                }
                updateEnabled();
            }
        }
        
    }
    
    
    /**
     * {@link Project} settings. 
     */
    class ProjectLauncherForm
            extends DefaultFormPage { 
        
        @Override
        public void createFormContents( IFormPageSite site ) {
            super.createFormContents( site );
            
            Composite body = site.getPageBody();
            body.setLayout( ColumnLayoutFactory.defaults()
                    .spacing( 5 /*panelSite.getLayoutPreference( LAYOUT_SPACING_KEY ) / 4*/ )
                    .margins( getSite().getLayoutPreference().getSpacing() / 2 ).create() );
        }
    }
    
}
