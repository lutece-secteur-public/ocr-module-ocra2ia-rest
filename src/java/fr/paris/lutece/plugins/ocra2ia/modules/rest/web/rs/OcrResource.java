/*
 * Copyright (c) 2002-2019, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.ocra2ia.modules.rest.web.rs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;

import fr.paris.lutece.plugins.ocra2ia.exception.OcrException;
import fr.paris.lutece.plugins.ocra2ia.service.OcrService;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.util.AppLogService;
import net.sf.json.JSONObject;

/**
 *
 * OcrResource
 *
 */
@Path( RestConstants.BASE_PATH + "ocr" )
public class OcrResource
{

    // i18n message
    private static final String MESSAGE_TEST_WS_OK        = "module.ocra2ia.rest.message.ws.test.ok";
    private static final String MESSAGE_ERROR_JSON_DATA   = "module.ocra2ia.rest.message.error.json.data";
    private static final String MESSAGE_ERROR_OCR_PROCESS = "module.ocra2ia.rest.message.error.ocr.process";

    // constants
    private static final String JSON_KEY_FILE_CONTENT     = "filecontent";
    private static final String JSON_KEY_FILE_EXTENSION   = "fileextension";
    private static final String JSON_KEY_DOCUMENT_TYPE    = "documenttype";
    private static final String JSON_KEY_MESSAGE          = "message";

    /**
     * File to parse with OCR
     */
    private byte[]              _byteFileContent;
    /**
     * File extension
     */
    private String              _strFileExtension;
    /**
     * DocumentType
     */
    private String              _strDocumentType;

    /**
     * OCR Service
     */
    @Inject
    private OcrService          _ocrService;

    /**
     * Web Service test to check if service is up.
     *
     * @return a message if service is up
     */
    @GET
    @Path( "/test" )
    @Produces( MediaType.APPLICATION_JSON )
    public String testIsUp( )
    {
        AppLogService.info( "service is up !!" );
        return new JSONObject( ).accumulate( JSON_KEY_MESSAGE, I18nService.getLocalizedString( MESSAGE_TEST_WS_OK, Locale.getDefault( ) ) ).toString( );
    }

    /**
     * Web Service to process OCR.
     *
     * @param request
     *            the http request
     * @param strJsonData
     *            json string data must content file content, file extension and document type
     * @return result of the OCR in json format
     */
    @POST
    @Path( "/start" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public String parseImageFile( @Context HttpServletRequest request, String strJsonData )
    {
        JSONObject jsonObject = new JSONObject( );
        Map<String, String> ocrResults = new HashMap<>( );
        try
        {
            if ( !controleJsonData( strJsonData ) )
            {
                return jsonObject.accumulate( JSON_KEY_MESSAGE, I18nService.getLocalizedString( MESSAGE_ERROR_JSON_DATA, request.getLocale( ) ) ).toString( );
            }
            // proceed OCR
            AppLogService.info( "OCR begin !!" );
            ocrResults = _ocrService.proceed( _byteFileContent, _strFileExtension, _strDocumentType );

        } catch ( IOException e )
        {
            AppLogService.error( e.getMessage( ), e );
            return jsonObject.accumulate( JSON_KEY_MESSAGE, I18nService.getLocalizedString( MESSAGE_ERROR_JSON_DATA, request.getLocale( ) ) ).toString( );
        } catch ( OcrException e )
        {
            AppLogService.error( e.getMessage( ), e );
            String[] messageArgs = { e.getMessage( ) };
            return jsonObject.accumulate( JSON_KEY_MESSAGE, I18nService.getLocalizedString( MESSAGE_ERROR_OCR_PROCESS, messageArgs, request.getLocale( ) ) ).toString( );
        }

        AppLogService.info( "OCR end !!" );
        jsonObject.accumulateAll( ocrResults );

        return jsonObject.toString( );
    }

    /**
     * Control of coherence of the json flux.
     *
     * @param strJsonData
     *            the json flux to control
     * @return true if json data is correct.
     * @throws IOException
     *             the IOException
     */
    private boolean controleJsonData( String strJsonData ) throws IOException
    {

        boolean res = false;

        ObjectMapper mapper = new ObjectMapper( );
        JSONObject jsonObject = mapper.readValue( strJsonData, JSONObject.class );

        if ( jsonObject.containsKey( JSON_KEY_FILE_CONTENT ) && jsonObject.containsKey( JSON_KEY_FILE_EXTENSION ) && jsonObject.containsKey( JSON_KEY_DOCUMENT_TYPE ) )
        {
            res = true;

            _strFileExtension = jsonObject.getString( JSON_KEY_FILE_EXTENSION );
            _byteFileContent = Base64.getDecoder( ).decode( jsonObject.getString( JSON_KEY_FILE_CONTENT ).getBytes( StandardCharsets.UTF_8 ) );
            _strDocumentType = jsonObject.getString( JSON_KEY_DOCUMENT_TYPE );

        }

        return res;
    }

}
