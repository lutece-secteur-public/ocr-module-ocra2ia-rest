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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;

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
    private static final String JSON_UTF8_CONTENT_TYPE    = "application/json; charset=UTF-8";

    /**
     * OCR Service
     */
    @Inject
    private OcrService          _ocrService;
    
    /**
     * Json object reader
     */
    private static final ObjectReader _jsonObjectReader = new ObjectMapper( ).reader(JSONObject.class);

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
     * @return result of the OCR in json format, 200 OK, 400 erreur client, 500 erreur serveur
     * 
     */
    @POST
    @Path( "/start" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response parseImageFile( @Context HttpServletRequest request, String strJsonData )
    {
        try
        {
            JSONObject jsonRequestObject = _jsonObjectReader.readValue( strJsonData );
        	
            if ( !controleJsonData( jsonRequestObject ) )
            {
                return clientErrorResponse(request, MESSAGE_ERROR_JSON_DATA);
            }
            
            String _strFileExtension = jsonRequestObject.getString( JSON_KEY_FILE_EXTENSION );
            byte[] _byteFileContent = Base64.getDecoder( ).decode( jsonRequestObject.getString( JSON_KEY_FILE_CONTENT ).getBytes( StandardCharsets.UTF_8 ) );
            String _strDocumentType = jsonRequestObject.getString( JSON_KEY_DOCUMENT_TYPE );
            
            // proceed OCR
            AppLogService.info( "OCR begin !!" );
            Map<String, String> ocrResults = _ocrService.proceed( _byteFileContent, _strFileExtension, _strDocumentType );
            AppLogService.info( "OCR end !!" );
            
            return ocrResultResponse(ocrResults);
            
        } catch ( IOException e )
        {
            AppLogService.error( e.getMessage( ), e );
            return clientErrorResponse(request, MESSAGE_ERROR_JSON_DATA);
        } catch ( OcrException e )
        {
            AppLogService.error( e.getMessage( ), e );
            return serverErrorResponse(request, MESSAGE_ERROR_OCR_PROCESS, e.getMessage( ));
        }
    }

	/**
	 * ocr Result Response
	 * 
	 * @param ocrResults the ocr result
	 * @return ok response 200 with json format of ocr result data
	 */
	private Response ocrResultResponse(Map<String, String> ocrResults) {
		JSONObject jsonObject = new JSONObject( );
		jsonObject.accumulateAll( ocrResults );
		return Response.ok().entity(jsonObject.toString()).build();
	}

	/**
	 * Client Error Response
	 * 
	 * @param request            the request
	 * @param messagePropertyKey the message Property Key
	 * @return bad request error 400
	 */
	private Response clientErrorResponse(HttpServletRequest request, String messagePropertyKey) {
		JSONObject jsonObject = new JSONObject( );
		jsonObject.accumulate( JSON_KEY_MESSAGE, I18nService.getLocalizedString( MESSAGE_ERROR_JSON_DATA, request.getLocale( ) ) );
		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
	}
	
	/**
	 * Server Error Response
	 * 
	 * @param request            the request
	 * @param messagePropertyKey the message Property Key
	 * @param messageArgs        the message Args
	 * @return server error 500
	 */
	private Response serverErrorResponse(HttpServletRequest request, String messagePropertyKey, String ... messageArgs) {
		JSONObject jsonObject = new JSONObject( );
		jsonObject.accumulate( JSON_KEY_MESSAGE, I18nService.getLocalizedString( MESSAGE_ERROR_OCR_PROCESS, messageArgs, request.getLocale( ) ) );
		return Response.serverError().entity(jsonObject.toString()).build();
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
    private boolean controleJsonData(JSONObject jsonObject)
    {
        return  jsonObject.containsKey( JSON_KEY_FILE_CONTENT ) && jsonObject.containsKey( JSON_KEY_FILE_EXTENSION ) && jsonObject.containsKey( JSON_KEY_DOCUMENT_TYPE ) ;
    }

}
