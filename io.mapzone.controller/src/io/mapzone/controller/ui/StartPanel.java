/* 
 * mapzone.io
 * Copyright (C) 2016, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package io.mapzone.controller.ui;

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.polymap.core.ui.FormDataFactory.on;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.polymap.core.runtime.UIThreadExecutor;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.security.SecurityContext;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormDataFactory.Alignment;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.PropertyAccessEvent;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.ConstraintLayout;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.LayoutSupplier;

import org.polymap.cms.ContentProvider;
import org.polymap.cms.ContentProvider.ContentObject;

import io.mapzone.controller.ui.user.RegisterPanel;
import io.mapzone.controller.um.repository.LoginCookie;
import io.mapzone.controller.um.repository.User;

/**
 * Landing page or open {@link DashboardPanel} if {@link LoginCookie} is set.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StartPanel
        extends CtrlPanel {

    private static Log log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier     ID = PanelIdentifier.parse( "start" );

    private Composite                       parent;

    
    @Override
    public boolean wantsToBeShown() {
        return getSite().getPath().size() == 1;
    }
    
    
    @Override
    public void createContents( @SuppressWarnings("hiding") Composite parent ) {
        this.parent = parent;
        site().setSize( SIDE_PANEL_WIDTH2, 550, Integer.MAX_VALUE );
        site().title.set( "mapzone" );
        
        // login cookie -> dashboard
        Optional<LoginCookie> loginCookie = LoginCookie.access().findAndUpdate();
        if (loginCookie.isPresent()) {
            User cookieUser = loginCookie.get().user.get();            
            userPrincipal.set( SecurityContext.instance().loginTrusted( cookieUser.name.get() ) );
            log.info( "Cookie user: " + userPrincipal.get().getName() );
            
            openDashboard();

            // frontpage, always created in case we get back from dashboard
            // but delay to avoid frontpage showing short before dashboard
            UIThreadExecutor.async( () -> {
                createFrontpageContents();
            });
        }
        else {
            userPrincipal.addListener( this, (PropertyAccessEvent ev) -> ev.getType() == PropertyAccessEvent.TYPE.SET );
            createFrontpageContents();
        }
    }


    protected void createFrontpageContents() {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 10 ).margins( 10, 0, 10, 0 ).create() );
        
        ContentProvider cp = ContentProvider.instance();
        List<String> specials = Lists.newArrayList( "1welcome", "99bottom" );
        
        // banner
        Composite banner = tk().createComposite( parent, SWT.BORDER );
        banner.setBackground( UIUtils.getColor( 60, 70, 80 ) );
        FormDataFactory.on( banner ).fill().noBottom();  //.height( 200 )
        banner.setLayout( FormLayoutFactory.defaults().margins( 0, 0 ).create() );
        
        Label welcome = new Label( banner, SWT.WRAP ) {
            @Override
            public Point computeSize( int wHint, int hHint, boolean changed ) {
                // suppress default text size computation in order to avoid flickering
                // caused by client side font size determination
                double area = 596 * 222;
                int height = Math.max( Math.min( (int)(area / wHint), 350 ), 250 );
                Point result = new Point( wHint, height );
                //log.info( "" + result );
                return result;
            }
        };
        tk().adapt( welcome, false, false );
        String welcomeContent = cp.findContent( "frontpage/1welcome.md" ).content();
        welcome.setText( tk().markdownToHtml( welcomeContent, welcome ) );
        on( welcome ).fill().left( 10 ).right( 60 );
        
        // btn
        Matcher matcher = Pattern.compile( "^button.text=(.+)$", Pattern.MULTILINE ).matcher( welcomeContent );
        String btnText = matcher.find() ? matcher.group( 1 ) : "Join..."; 
        Composite btnContainer = tk().createComposite( banner );
        FormDataFactory.on( btnContainer ).fill().left( 65 ).right( 100 );
        btnContainer.setLayout( FormLayoutFactory.defaults().create() );
        Control filled = on( tk().createComposite( btnContainer ) ).fill().control();
        Button btn = on( tk().createButton( btnContainer, btnText, SWT.PUSH ) )
                .top( filled, 0, Alignment.CENTER ).left( 10 ).height( 45 ).width( 135 ).control();
        btn.setToolTipText( "Create an accountTest drive mapzone for free" );
        btn.moveAbove( null );
        btn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                getContext().openPanel( site().path(), RegisterPanel.ID );
            }
        });
        welcome.moveAbove( null );
        
        // article grid
        Composite grid = on( tk().createComposite( parent ) ).fill().top( banner ).bottom( 100, -80 ).control();
        LayoutSupplier layoutPrefs = BatikApplication.instance().getAppDesign().getPanelLayoutPreferences();
        grid.setLayout( new ConstraintLayout( layoutPrefs ).setMaxColumns( 3 ) );

        for (ContentObject co : cp.listContent( "frontpage" ) ) {
            if (co.contentType().startsWith( "text" ) && !specials.contains( co.name() ) ) {
                createArticleSection( grid, co );
            }
        }
        
        // bottom links
        Composite bottom = on( tk().createComposite( parent ) ).fill().top( 100, -40 ).control();
        bottom.setLayout( FormLayoutFactory.defaults().create() );
        filled = on( tk().createComposite( bottom )).fill().control();
        on( tk().createFlowText( bottom, cp.findContent( "frontpage/99bottom.md" ).content() ) )
                .fill().left( filled, 0, Alignment.CENTER )
                .control().moveAbove( null );
    }

    
    protected void createArticleSection( Composite grid, ContentObject co ) {
        String content = co.content();
        String title = co.title();
        if (content.startsWith( "#" )) {
            title = substringBefore( content, "\n" ).substring( 1 );
            content = content.substring( title.length() + 2 );
        }

        IPanelSection section = tk().createPanelSection( grid, title, SWT.BORDER );
        
        int priority = 100 - Integer.parseInt( co.name().substring( 0, 2 ) );
        section.getControl().setLayoutData( new ConstraintData()
                .prio( priority ).minHeight( 300 ).minWidth( 350 ) ); //.maxWidth( 450 ) );
                //ColumnDataFactory.defaults().heightHint( 300 ).widthHint( 350 ).create() );
        
        section.getBody().setLayout( FormLayoutFactory.defaults().create() );

        // this generates an iFrame with proper size; this allows to load
        // scripts/CSS in content *and* is better than a Label which always has
        // its own idea of its size depending on fonts in content
        Browser b = new Browser( section.getBody(), SWT.NONE );
        on( b ).fill().width( 380 );
        
        // XXX moved <head> elements in a <p> which generates a margin on top
        String html = tk().markdownToHtml( content, b );
        b.setText( html );
    }
    
    
    /**
     * Page layout: 800px width    
     */
    protected void createPageLayout() {
        ((ConstraintLayout)parent.getLayout()).setMargins( new LayoutSupplier() {
            LayoutSupplier layoutPrefs = site().layoutPreferences();
            LayoutSupplier appLayoutPrefs = BatikApplication.instance().getAppDesign().getAppLayoutSettings();
            @Override
            public int getSpacing() {
                return 0; //layoutPrefs.getSpacing() * 2;
            }
            protected int margins() {
                Rectangle bounds = parent.getParent().getBounds();
                int availWidth = bounds.width-(appLayoutPrefs.getMarginLeft()+appLayoutPrefs.getMarginRight());
                return Math.max( (availWidth-800)/2, layoutPrefs.getMarginLeft());
            }
            @Override
            public int getMarginTop() { return layoutPrefs.getMarginTop(); }
            @Override
            public int getMarginRight() { return margins(); }
            @Override
            public int getMarginLeft() { return margins(); }
            @Override
            public int getMarginBottom() { return layoutPrefs.getMarginBottom() /*+ 10*/; }
        });
    }
    
    
    protected void openDashboard() {
        // make StartPanel/frontpage to big to be shown beside the dashboard
        site().preferredWidth.set( Integer.MAX_VALUE );
        site().minWidth.set( Integer.MAX_VALUE );

        UIThreadExecutor.async( () -> getContext().openPanel( getSite().getPath(), DashboardPanel.ID ) );
    }


    /** 
     * Triggered by {@link StartPanel} and {@link RegisterPanel}.
     */
    @EventHandler( display=true )
    protected void userLogedIn( PropertyAccessEvent ev ) {
        openDashboard();    
    }
    
}
