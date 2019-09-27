package tsurumai.workflow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import tsurumai.workflow.util.ServiceLogger;

/**データファイルのキャッシュ*/
public  class CacheControl<T> {

	protected T cached = null;

	protected String file = null;
	
	protected Date cachedDate = new Date();

	public String getTargetFile() {
		return this.file;
	}
	public T get(){
		return cached;
	}
	protected static ServiceLogger logger = ServiceLogger.getLogger();
	public T set(final T data){
		cached = data;
		logger.debug("キャッシュデータを更新しました。" + this.file + ":"+ data.getClass().getSimpleName());
		return cached;
	}
	/**タイムスタンプを更新*/
	public void reload(final String file){
		File target = new File(file);
		if(!target.exists() || target.isDirectory())
			logger.warn("データファイルが不正です。" + target.getAbsolutePath());
		this.file = file;
		this.cachedDate = new Date(target.lastModified());

		
		logger.debug("キャッシュデータが更新されています。" + this.file + ":" + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(this.cachedDate));
	}
	/**更新されていたらnull、されていなければキャッシュされた値を返す。
	 * @param file ファイル名
	 * @return nullまたはファイル内容 
	 * */
	public T load(final String file) {
		if(cached == null || this.file == null || new File(this.file).compareTo(new File(file)) != 0) {
			reload(file); return null;
		}
		if(this.cachedDate.getTime() != new File(file).lastModified()){
			reload(file); return null;
		}
		return get();
	}

}
