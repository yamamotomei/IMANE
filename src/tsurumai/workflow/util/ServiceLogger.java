package tsurumai.workflow.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import javax.inject.Singleton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;


/**ログ出力を制御する
 * 
 * ログ出力にはlog4jを使用する。設定方法については<a href = "https://logging.apache.org/log4j/1.2">ドキュメント</a>を参照。
 * ロガー名はデフォルトで"workshop"。 システムプロパティFrICORE.logger.nameで変更可能・。<br>
 * */
public class ServiceLogger {
	ServiceLogger() {

	}
	@Singleton
	static ServiceLogger theInstance = null;
	public static ServiceLogger getLogger() {
		if(theInstance == null) {
			theInstance = new ServiceLogger();
//			String loggerName = System.getProperty(LOGGER_NAME,"workshop");
			theInstance.logger = LogFactory.getLog("workshop");

				Appender a = ((Log4JLogger)theInstance.logger).getLogger().getAppender("file");
				if(a instanceof FileAppender) {
					FileAppender app =(FileAppender)a;
					theInstance.logger.info("logging to: " + new File(app.getFile()).getAbsolutePath());
				}
		}
		return theInstance;
	}
	public static final String LOGGER_NAME = "FrICORE.logger.name";
//	private static Log logger = LogFactory.getLog(System.getProperty(LOGGER_NAME,"workshop"));
	private Log logger = null;
	protected static SimpleDateFormat tm = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public void log(final String message) {
		
		String header = tm.format(new Date()) + ": ";
		//ps.println(header + ": ["+level + "] " + message);
	//	logger.log(Priority., message);
		logger.info(header +message);
	}
	public void info(final String msg){
		String header = tm.format(new Date()) + ": ";
		logger.info(header + msg);
	}

	public void warn(final String message, final Throwable cause) {
		String header = tm.format(new Date()) + ": ";
		logger.warn(header + message, cause);
	}
	public void warn(final String message) {
		String header = tm.format(new Date()) + ": ";
		warn(header + message, null);
	}

	public void error(final String message, final Throwable cause) {
		String header = tm.format(new Date()) + ": ";
		logger.error(header + message , cause);
	}

	public void error(final String message) {
		String header = tm.format(new Date()) + ": ";
		logger.error(header + message);
	}
	public void debug(final String message, final Throwable cause) {
		String header = tm.format(new Date()) + ": ";
		logger.debug(header + message, cause);
	}
	public void error(final Throwable cause){
		logger.error(cause.getMessage(), cause);
	}
	public void debug(final String message) {
		String header = tm.format(new Date()) + ": ";
		logger.debug(header + message);
	}
	public static void main(String[] args) {
		ServiceLogger.getLogger().info("hogehoge");
	}
}
