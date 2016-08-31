/*
 * polymap.org Copyright (C) 2016, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package io.mapzone.arena.share;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import java.net.URL;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.rap.rwt.RWT;

import org.polymap.core.operation.DefaultOperation;
import org.polymap.core.operation.OperationSupport;
import org.polymap.core.runtime.i18n.IMessages;
import org.polymap.core.ui.ColumnDataFactory;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;

import org.polymap.rap.updownload.download.DownloadService.ContentProvider;

import io.mapzone.arena.ArenaPlugin;
import io.mapzone.arena.Messages;
import io.mapzone.arena.share.content.ArenaContentProvider.ArenaContent;
import io.mapzone.arena.share.content.ImagePngContentProvider.ImagePngContent;
import io.mapzone.arena.share.content.ShareableContentProvider;

/**
 * Sharelet to send an email with a link to arena and an embedded image.
 *
 * @author Steffen Stundzig
 */
public class EMailSharelet
        extends Sharelet {

    private static Log             log  = LogFactory.getLog( EMailSharelet.class );

    private static final IMessages i18n = Messages.forPrefix( "EMailSharelet" );

    private ContentProvider        provider;

    private Session                session;


    @Override
    public void init( ShareletSite site ) {
        site.title.set( i18n.get( "title" ) );
        site.description.set( i18n.get( "description" ) );
        site.priority.set( 211 );
        site.image.set( ArenaPlugin.images().svgImage( "email.svg", SvgImageRegistryHelper.NORMAL48 ) );
        super.init( site );
    }


    private ShareletSectionProvider mailform() {
        return new ShareletSectionProvider() {

            @Override
            public String title() {
                return i18n.get( "email_title" );
            }


            @Override
            public String[] supportedTypes() {
                return new String[] { "image/png", "application/arena" };
            }


            @Override
            public void createContents( final Composite parent, ShareableContentProvider... contentBuilders ) {
                ImagePngContent image = (ImagePngContent)contentBuilders[0].get();
                ArenaContent arena = (ArenaContent)contentBuilders[1].get();
                Button fab = tk().createFab();

                ColumnDataFactory.on( tk().createLabel( parent, i18n.get( "email_to" ), SWT.WRAP ) ).widthHint( preferredWidth( parent ) );

                Text to = tk().createText( parent, "", SWT.BORDER, SWT.WRAP );
                to.addModifyListener( new ModifyListener() {

                    @Override
                    public void modifyText( ModifyEvent event ) {
                        // TODO add check for correct email adresses here
                        if (!StringUtils.isBlank( to.getText().trim() )) {
                            fab.setVisible( true );
                            fab.setEnabled( true );
                        }
                        else {
                            fab.setVisible( false );
                            fab.setEnabled( false );
                        }
                    }
                } );
                ColumnDataFactory.on( to ).widthHint( preferredWidth( parent ) ).heightHint( 40 );

                ColumnDataFactory.on( tk().createLabel( parent, i18n.get( "email_subject" ), SWT.WRAP ) ).widthHint( preferredWidth( parent ) );

                Text subject = tk().createText( parent, "", SWT.BORDER );
                ColumnDataFactory.on( subject ).widthHint( preferredWidth( parent ) );

                ColumnDataFactory.on( tk().createLabel( parent, i18n.get( "email_message" ), SWT.WRAP ) ).widthHint( preferredWidth( parent ) );

                Text message = tk().createText( parent, i18n.get( "email_message_text", arena.arena ), SWT.BORDER, SWT.WRAP );
                ColumnDataFactory.on( message ).widthHint( preferredWidth( parent ) ).heightHint( 120 );

                Button attachPreview = tk().createButton( parent, "", SWT.CHECK );
                attachPreview.setText( i18n.get( "email_preview" ) );
                attachPreview.setSelection( true );
                ColumnDataFactory.on( attachPreview ).widthHint( preferredWidth( parent ) );

                StringBuffer oreview = new StringBuffer( "<img width='" ).append( image.previewWidth ).append( "' height='" ).append( image.previewHeight ).append( "' src='" );
                oreview.append( image.previewResource );
                oreview.append( "'/>" );

                Label l = tk().createLabel( parent, oreview.toString(), SWT.BORDER );
                l.setData( RWT.MARKUP_ENABLED, Boolean.TRUE );
                ColumnDataFactory.on( l ).widthHint( Math.min( preferredWidth( parent ), image.previewWidth ) ).heightHint( image.previewHeight );

                fab.setVisible( false );
                fab.addSelectionListener( new SelectionAdapter() {

                    @Override
                    public void widgetSelected( SelectionEvent e ) {
                        final String toText = to.getText();
                        final String subjectText = subject.getText();
                        final String messageText = message.getText();
                        final boolean attach = attachPreview.getSelection();
                        DefaultOperation op = new DefaultOperation( i18n.get( "email_title" ) ) {

                            @Override
                            public IStatus doExecute( IProgressMonitor monitor, IAdaptable info ) throws Exception {
                                sendEmail( toText, subjectText, messageText, attach, image );
                                return Status.OK_STATUS;
                            }
                        };
                        // execute
                        OperationSupport.instance().execute2( op, true, false );
                        fab.setVisible( false );
                    }
                } );
            }
        };
    }


    private void sendEmail( final String toText, final String subjectText, final String messageText,
            final boolean withAttachment,
            final ImagePngContent image ) throws Exception {
        MimeMessage msg = new MimeMessage( mailSession() );

        msg.addRecipients( RecipientType.TO, InternetAddress.parse( toText, false ) );
        // TODO we need the FROM from the current user
        msg.addFrom( InternetAddress.parse( "steffen@mapzone.io" ) );
        msg.setReplyTo( InternetAddress.parse( "steffen@mapzone.io" ) );

        msg.setSubject( subjectText, "utf-8" );
        if (withAttachment) {
            // add mime multiparts
            Multipart multipart = new MimeMultipart();

            BodyPart part = new MimeBodyPart();
            part.setText( messageText );
            multipart.addBodyPart( part );

            // Second part is attachment
            part = new MimeBodyPart();
            part.setDataHandler( new DataHandler( new URLDataSource( new URL( image.imgResource ) ) ) );
            part.setFileName( "preview.png" );
            part.setHeader( "Content-ID", "preview" );
            multipart.addBodyPart( part );

            // // third part in HTML with embedded image
            // part = new MimeBodyPart();
            // part.setContent( "<img src='cid:preview'>", "text/html" );
            // multipart.addBodyPart( part );

            msg.setContent( multipart );
        }
        else {
            msg.setText( messageText, "utf-8" );
        }
        msg.setSentDate( new Date() );
        Transport.send( msg );
    }


    private Session mailSession() throws Exception {
        if (StringUtils.isBlank( System.getProperty( "mail.share.user" ) )) {
            throw new RuntimeException( "Sytem property mail.share.user must be set" );
        }
        if (StringUtils.isBlank( System.getProperty( "mail.share.password" ) )) {
            throw new RuntimeException( "Sytem property mail.share.password must be set" );
        }
        if (session == null) {
            Properties props = new Properties();
            props.put( "mail.smtp.host", System.getProperty( "mail.smtp.host", "mail.mapzone.io" ) );
            props.put( "mail.smtp.port", "25" );
            props.put( "mail.smtp.auth", "true" );
            // TODO uncomment if the mail server contains a correct SSL certificate
            // props.put( "mail.smtp.starttls.enable", "true" ); // enable STARTTLS

            // create Authenticator object to pass in Session.getInstance argument
            Authenticator auth = new Authenticator() {

                // override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication( System.getProperty( "mail.share.user" ), System.getProperty( "mail.share.password" ) );
                }
            };
            session = Session.getInstance( props, auth );
        }
        return session;
    }


    private int preferredWidth( Composite parent ) {
        return Math.min( parent.getDisplay().getClientArea().width, site().preferredWidth.get() ) - 50;
    }


    public List<ShareletSectionProvider> sections() {
        return Lists.newArrayList( mailform() );
    }
}
