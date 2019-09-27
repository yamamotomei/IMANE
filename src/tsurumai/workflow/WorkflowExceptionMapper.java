package tsurumai.workflow;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**例外クラスとHTTPステータスのマッピングを管理する*/
@Provider
public class WorkflowExceptionMapper implements ExceptionMapper<WorkflowException>{


	@Override
	public Response toResponse(final WorkflowException exception) {
		if(exception.getResponse().getStatus() != 0){
			return Response.status(exception.getResponse().getStatus()).build();
		}else {
			return Response.status(exception.getHttpStatus()).build();
		}
	}

}
