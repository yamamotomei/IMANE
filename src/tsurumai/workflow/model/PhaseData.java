package tsurumai.workflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import tsurumai.workflow.CacheControl;
import tsurumai.workflow.WorkflowException;
import tsurumai.workflow.WorkflowService;
import tsurumai.workflow.util.ServiceLogger;
import tsurumai.workflow.util.Util;
@XmlRootElement
@JsonIgnoreProperties({"comment","","//","/*","*/","#","<!--","-->"})
//@JsonInclude(JsonInclude.Include.NON_NULL)
/**演習フェーズの定義を表現します。*/
public class PhaseData {
	@XmlAttribute
	public int phase;
	@XmlAttribute
	public String description;
	@XmlAttribute
	public int timelimit = 1200;
	@XmlAttribute
	public int[] endstate;

	@XmlAttribute
	public String copyright;
	@XmlAttribute
	public String version;
	@XmlAttribute
	public String author;
	@XmlAttribute
	public String created;
	@XmlAttribute
	public String updated;


	protected static CacheControl<List<PhaseData>> cache = new CacheControl<>();
	protected static ServiceLogger logger = ServiceLogger.getLogger();

	public static void reload(final String file){
		cache.reload(file);
		logger.info("フェーズデータを再ロードしました。"+ file);
	}
	public static synchronized List<PhaseData> loadAll() throws WorkflowException{

		List<PhaseData> def =Util.dataExists("sys/setting.json") ?
				load(WorkflowService.getContextRelativePath("sys/setting.json") ) : new ArrayList<>();
		List<PhaseData> cust = load(cache.getTargetFile());
		List<PhaseData> all = new ArrayList<>();
		all.addAll(def);
		all.addAll(cust);
		return all;
	}
	/**フェーズ定義をロードする*/
	public static synchronized List<PhaseData> load(final String path) throws WorkflowException{
		try{
			cache.reload(path);//
			String contents = Util.readAll(path);
			ArrayList<PhaseData> ret = new ArrayList<>();
			JSONArray arr  = new JSONObject(contents).getJSONArray("phases");
			for(java.util.Iterator<Object> i = arr.iterator();i.hasNext();){
				Object o = i.next();
				PhaseData cur = (PhaseData)new ObjectMapper().readValue(o.toString(), PhaseData.class);
				ret.add(cur);
			}
			Collections.sort(ret, new Comparator<PhaseData>() {
				@Override
				public int compare(PhaseData o1, PhaseData o2) {
					if(o1.phase == o2.phase)return 0;
					else if(o1.phase > o2.phase)return 1;
					else	return -1;
				}
			});
			cache.set(ret);
			return ret;
			//return ret.toArray(new PhaseData[ret.size()]);
		}catch(Throwable t){
			throw new WorkflowException("フェーズの初期化に失敗しました。" , t);
		}
	}



}
