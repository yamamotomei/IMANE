package tsurumai.workflow;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;



/**シナリオデータの異常*/
class ScenarioException extends WorkflowException{
	private static final long serialVersionUID = 1L;
	String jsonfile = "N/A";
	public ScenarioException() {
		super(HttpServletResponse.SC_BAD_REQUEST);
	}
	public ScenarioException(final String message, final String jsonfile, final Throwable t){
		super(message, HttpServletResponse.SC_BAD_REQUEST, t);
		this.jsonfile = jsonfile;
		if(t instanceof JsonParseException){
			//TODO:
		}else if(t instanceof JsonProcessingException){

		}else if(t instanceof JsonMappingException){
			JsonMappingException j = (JsonMappingException)t;
			j.getLocalizedMessage();
		}
	}
	public ScenarioException(final String message){
		super(message, HttpServletResponse.SC_BAD_REQUEST);
	}
	@Override
	public String getMessage() {
		return super.getMessage() + "[" +jsonfile +"]";
	}
}
class WorkflowParameterException extends WorkflowException{
	public WorkflowParameterException() {
		super(HttpServletResponse.SC_BAD_REQUEST);
	}
	public WorkflowParameterException(final String msg) {super(msg, HttpServletResponse.SC_BAD_REQUEST);}
}
class WorkflowSessionException extends WorkflowException{
	private static final long serialVersionUID = 1L;
	
	public WorkflowSessionException() {
		super(HttpServletResponse.SC_UNAUTHORIZED);
	}
	public WorkflowSessionException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
	public WorkflowSessionException(final String msg) {
		this(msg, null);
	}
	public WorkflowSessionException(final String msg, int status, final Throwable cause, boolean nostacktrace) {
		super(msg, status,cause, nostacktrace);
	}
}

public class WorkflowException extends WebApplicationException{
	protected int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final long serialVersionUID = 1L;
	
	String clientId = "";
	public WorkflowException() {
		super();
	}
	public WorkflowException(final String msg) {
		this(msg, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
	public WorkflowException(int status) {
		this(String.valueOf(status), status, null);
	}
	public WorkflowException(final String msg, final Throwable cause) {
//		super(msg, cause);
		this(msg, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause, false);	
		if(cause instanceof UnrecognizedPropertyException){
		}
		
	}
	public WorkflowException(final String msg, int status) {
		this(msg, status, null);
	}
	public WorkflowException(final String msg, int status, boolean nostacktrace) {
		this(msg, status, null, nostacktrace);
	}
	public WorkflowException(final String msg, int status, final Throwable cause) {
		this(msg, status, cause, false);
	}
	public WorkflowException(final String msg, int status, final Throwable cause, boolean nostacktrace) {
		super(msg, cause, status);
		if(!nostacktrace){
			System.err.println(msg);
			this.printStackTrace();
		}
	}
	public WorkflowException(int status, final Throwable cause) {
		this(String.valueOf(status), status, cause);
		this.status =  status;
	}
	public void setDetail(final String clientid){
		this.clientId = clientid;
	}
	public int getHttpStatus() {return this.status;}
	public String serialize(){
		CharArrayWriter out =new CharArrayWriter();
		PrintWriter ps  = new PrintWriter(out);
		this.printStackTrace(ps);
		JSONObject j = new JSONObject();
		j.put("clientid", clientId != null ? clientId : "<unknown>")
			.put("status",  super.getResponse().getStatus())
			.put("message",getMessage())
			.put("kind", getClass().getSimpleName())
			.put("cause", out.toString());
		return j.toString();
	}
	@Override
	public Response getResponse() {
		String msg = this.serialize();
		return Response.status(super.getResponse().getStatus()).entity(msg).type(MediaType.APPLICATION_JSON).build();
	}
}
