package tsurumai.workflow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import tsurumai.workflow.model.CardData;
import tsurumai.workflow.model.Member;
import tsurumai.workflow.model.PointCard;
import tsurumai.workflow.model.ReplyData;
import tsurumai.workflow.model.StateData;
import tsurumai.workflow.util.Pair;
import tsurumai.workflow.util.ServiceLogger;
import tsurumai.workflow.util.Util;
@JsonIgnoreProperties({"comment","","//","#"})
//@JsonInclude(JsonInclude.Include.NON_NULL)

/**操業レベル定義
 * @deprecated 操業レベルに関する処理は廃止されました*/
class OperationLevelDef implements Comparable<OperationLevelDef>{

        /**フェーズ開始からの経過時間(秒)**/public int time;
        /**操業レベル*/public int level;

        //降順でソート
        @Override
        public int compareTo(OperationLevelDef o) {
                return -Integer.compare(this.time, o.time);
        }

}
@JsonIgnoreProperties({"comment","","//","#"})
/**フェーズにおける操業状態の定義*/
 class OperationStateDef{
        public OperationStateDef(){}
        /**フェーズ*/public int phase;
        /**このフェーズの操業状態の定義*/public OperationLevelDef[] state;
        /**経過時間の降順でソートした操業レベルを返す*/
        public OperationLevelDef[] getState(){
                Collections.sort(Arrays.asList(this.state));
                return this.state;
        }
        /**現在のフェーズの開始からの経過時間にもとづいて、現在の操業レベルを返す*/
        public OperationLevelDef getOperationLevel(int phase, final Date started){
                if(phase != this.phase) return null;
                long elapsed = (new Date().getTime() - started.getTime())/1000 ;//経過時間(s)
                OperationLevelDef[] def = getState();
                for(OperationLevelDef d : def){//降順でソートされている
                        if(elapsed > d.time){
                                return d;
                        }
                }
                return null;
        }
}
/**トリガーイベントを表現する
 *
 * */
@JsonIgnoreProperties({"comment","","//","#"})
class TriggerEvent{
        public TriggerEvent(){}
        public String state;
        public Date date;
        public String from;
        public String to;

        public TriggerEvent(String state, final String from, final String to){
                this.state = state;
                this.date = new Date();
                this.from = from;
                this.to= to;
        }

}
/**ワークフローのインスタンスを管理する
 *
 * チーム別のワークフロー状態、アクションキュー、履歴などを管理
 * */
@JsonIgnoreProperties({"comment","","//","#"})
@XmlRootElement
public class WorkflowInstance {
        public WorkflowInstance() {}
        protected OperationStateDef[] operationStateDef = new OperationStateDef[0];
        static ServiceLogger logger =  ServiceLogger.getLogger();
        int interval = Integer.parseInt(System.getProperty("check.interval","1000"));
        protected CopyOnWriteArrayList<TriggerEvent> triggerEvents =  new CopyOnWriteArrayList<>();
        /**アクション要求のキュー*/
        protected CopyOnWriteArrayList<NotificationMessage> actionQueue = new CopyOnWriteArrayList<>();
        /**アクション実行結果の履歴*/
        protected CopyOnWriteArrayList<NotificationMessage> history = new CopyOnWriteArrayList<>();

        /**システム状態(ステートカード) Map&lt;ステートID、登録日時&gt; 登録日時はシリアライズ/でシリアライズの関係でDateまたはLongとなる*/
        protected Map<String, Object> systemState = new HashMap<>();

        protected synchronized void addState(String state, Object data, final String msgid){
                StateData s = StateData.getStateData(state);
                if(s == null){
                        logger.warn("存在しないシステムステートを追加しようとしました。" + state.toString());
                        s = new StateData(state);
                }
                logger.info("システムステートを追加します。" + s.toString());
                systemState.put(state, data);
                onAddState(state, msgid);
        }
        /**指定されたシステムステートが登録されているか*/
        protected synchronized boolean hasState(String stateid){
                for(String cur : this.systemState.keySet()){
                        if(cur.equals(stateid)) return true;
                }
                return false;
        }
        /**システムステートを持つか、OR評価。statesが空ならtrue(無条件)*/
        protected synchronized boolean hasStates(String[] states){
                if(states == null || states.length == 0)return true;
                for(String i : states){
                        if(hasState(i)) return true;
                }
                return false;
        }
        /**システムステートを持つか、AND評価。statesが空ならtrue(無条件)*/
        protected synchronized boolean hasAllStates(String[] states){
                if(states == null || states.length == 0) return true;
                for(String i : states){
                        if(!hasState(i)) return false;
                }
                return true;
        }
        /**システムステートを削除する*/
        protected synchronized void removeState(final String state){
                StateData s = StateData.getStateData(state);
                if(s == null){
                        logger.warn("存在しないシステムステートを削除しようとしました。" + state.toString());return;
                }
                if(hasState(state)){
                        logger.info("システムステートを削除します。" + s.toString());
                        systemState.remove(state);
                        onRemoveState(state);
                }else{
                        logger.info("指定されたシステムステートは割り当てられていません。" + s.toString());
                }
        }
        /**ワークフローの状態*/
        public interface State{
                public static String STARTED ="Started";
                public static String ENDED ="Ended";
                public static String SUSPENDED ="Suspended";
                public static String ABORTED = "Aborted";
                public static String NONE = "None";
        }
        /**操業レベルの定義*/
        public interface OperationLevel{
                /**最高*/         public static int HIGHEST = 8;
                /**良好*/         public static int HIGH = 7;
                /**通常*/         public static int NORMAL= 6;
                /**やや低*/        public static int  LOW= 5;
                /**低*/                  public static int LOWER = 4;
                /**最低*/         public static int LOWEST = 3;
                /**致命的*/        public static int CRITICAL = 2;
                /**緊急事態*/       public static int FATAL = 1;
                /**緊急停止(演習終了)*/ public static int  FAIL = 0;
        }
        /**ワークフローの割当て先チーム名
         */
        @XmlAttribute
        public String team = "";
        /**ワークフローのフェーズ*/
        @XmlAttribute
        public int phase = 0;
        /**インスタンスの状態*/
        @XmlAttribute
        public String state = State.NONE;

        /**WFインスタンスの開始時間*/
        @XmlAttribute
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public Date start = null;
        @XmlAttribute
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public Date saved = null;

        /**現在の操業レベル*/
        @XmlAttribute
        public int operationlevel = 0;
        @XmlAttribute
        public long pid = 0;

        @XmlAttribute
        public int score = 0;
        @XmlAttribute
        public Hashtable<String, String> properties = new Hashtable<>();

        /**獲得したポイントカード: イベントのIDとポイントカードのハッシュ*/
        @XmlElement
        public Hashtable<String, Collection<PointCard>>pointchest = new Hashtable<>();

        /**定義済ポイントカード*/
        @XmlElement
        public PointCard[] pointcards = null;

        /**ユーザの所有ステートカード。ユーザIDとステートIDのマップ。*/
        protected Map<String, Set<String>> memberStates =new HashMap<>();
        /**自動応答ユーザのステートカード状態を初期化する*/
        protected void initMemberStatus(){
                memberStates.clear();
                List<Member> members = Member.getTeamMembers(this.team);
                for(Member m : members){
                        memberStates.put(m.email, new HashSet<>());
                }
        }
        /**自動応答ユーザのステートカードを更新する。*/
        protected void updateUserStates(final NotificationMessage msg){
                Set<String> states = msg.fetchStatecards();
                if(states == null || states.isEmpty()) return;

                Set<String> members = msg.fetchRecipients(this.team);
                if(members == null || members.isEmpty())return;

                for(String m : members){
                        Set<String> cur = this.memberStates.get(m);
                        for(String s : states){
                                if(!cur.contains(s)){
                                        logger.info(String.format("[%s]がステートカード[%s]を獲得", m, s));
                                        cur.add(s);
                                }
                        }
                }
        }

        protected WorkflowService caller = null;
        protected Worker worker = null;
        public static WorkflowInstance newInstance(WorkflowService caller, final String team, long pid){
                WorkflowInstance inst = newInstance(caller.getScenarioDirectory(), team, pid);
                inst.caller = caller;
                inst.team = team;
                return inst;
        }
        /**
         * ワークフローインスタンスを初期化
         * @param basedir シナリオセット(チーム定義)の格納先
         * @param team チームID
         * */
        public static WorkflowInstance newInstance(String basedir, final String team, long pid){
                WorkflowInstance inst = new WorkflowInstance();
                inst.team = team;
                inst.initialize(basedir);
                inst.pid = pid;//TODO: 2019,9,4
                return inst;
        }
        public static WorkflowInstance newInstance(String basedir, final String team){
                return newInstance(basedir, team, -1);
        }

        public String toString(){
                return String.format("ワークフローインスタンス: %s phase %d; %s",  this.team, this.phase,
                                this.start != null ? new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(this.start) : "not started");
        }
        /**フローを初期化
         *@param basedir データファイル(operationstate.json)の格納先
         * */
        protected void initialize(final String basedir){
                try{
                        logger.info("initialize:" +this.toString());
                        ObjectMapper mapper = new ObjectMapper();

                        String path = basedir + File.separator + "operationstate.json";
                        if(new File(path).exists()){
                                String  c = Util.readAll(path);
                                OperationStateDef[] def = mapper.readValue(c,  OperationStateDef[].class);
                                this.operationStateDef =def;
                        }
                }catch(Throwable t){
                        throw new RuntimeException("failed to load operation state.", t);
                }       
        }

        /**フェーズを開始*/
        public void start(int phase){
                logger.info("start:" +this.toString());

                this.clearAutoActionTimers();
                this.state = State.STARTED;
                this.phase = phase;
                this.score = 0;
                this.start = new Date();
                this.actionQueue.clear();
                this.history.clear();
                this.triggerEvents.clear();
                this.systemState.clear();
                this.pointchest.clear();
                this.pointHistory.clear();
                this.autoActionHistory.clear();
                this.pointcards = PointCard.list(this.phase);
                this.initMemberStatus();
                if(this.worker != null){
                        this.worker.stop();
                }
//              try {
//                      Thread.sleep(1000);
//              }catch(Throwable t) {}
//             
                this.worker = new Worker();
                this.worker.start();
                logger.info("workflow started:" +this.toString());
        }
        /**フェーズを終了*/
        public void end(){
                logger.info("end:" +this.toString());
                if(this.worker != null){
                        this.worker.stop();
                        this.worker = null;
                }
                this.state =  State.ENDED;
                logger.info("workflow ended:" +this.toString());
        }

        /**一時停止(使用するかどうか？)*/
        public void suspend(){
                logger.info("suspend:" +this.toString());
                this.state = State.SUSPENDED;
                this.saved  = new Date();
                if(this.worker != null){
                        this.worker.stop();
                }
        }

        /**一時停止から復帰(使用するかどうか？)*/
        public void resume(){
                logger.info("resume:" +this.toString());
                this.state = State.STARTED;

                this.worker = new Worker();
                this.worker.start();
                logger.info("resumed:" +this.toString());
        }

        /**フェーズを中断・異常終了*/
        public void abort(){
                logger.info("abort:" +this.toString());
                this.state = State.ABORTED;

                for(Timer tm : this.autoActionTimers) {
                        tm.cancel();
                }
                this.autoActionTimers.clear();

                this.worker.stop();

                logger.info("workflow aborted:" +this.toString());

        }
        /**フェーズ状態変更のステートカードIDを返す
         * アクションIDは、10XXYY の形式(XX:フェーズ、YY: phaseState: PhaseStateのいずれか)*/
        public static int makePhaseStateID(int phase, int phaseState){
                int r = Integer.parseInt(String.format("10%02d%02d", phase, phaseState));
                logger.info("ステート変更:" + String.valueOf(r));
                return r;
        }
        public int makePhaseState(int phaseState){
                return makePhaseStateID(this.phase, phaseState);
        }

        protected boolean isRunning(){return State.STARTED.equals(this.state);}

        /**アクション要求を処理する
         * */
        public void requestAction(CardData action, Member from, Member to, Member[] cc, NotificationMessage reply) throws WorkflowException{
                prepareAction(action, from, to, cc, reply);
        }

        /**アクション要求を受け付ける
         * */
        protected void acceptAction(CardData action, Member from, Member to, Member[] cc, NotificationMessage reply){

                logger.info("action accepted. from:" +
                                (from !=null ? from.toString() : "nul") +",to:"+ (to != null ? to.toString()  : "null") +
                                        ",cc:" + Util.toString(cc!= null ?  cc.toString() : "null") + ",action:"+action.toString());

                NotificationMessage msg  = new NotificationMessage(this.pid, action, to, from, cc, reply);
                msg.team = this.team;
                enqueueAction(msg);

        }
        /**アクション要求を拒否する*/
        protected void rejectAction(CardData action, Member from, Member to, Member[] cc, NotificationMessage reply, String reason){
                logger.info("action rejected:" + action.toString());
                throw new WorkflowException("このアクションは実行できません。" + reason, HttpServletResponse.SC_BAD_REQUEST);
        }

        /**受信したアクションの事前チェック
         *  パスしたらキューに入れて復帰
         * */
        protected void prepareAction(CardData action, Member from, Member to, Member[] cc, NotificationMessage reply) throws WorkflowException{

                if(!this.isRunning()){
                        rejectAction(action, from, to, cc,reply,  "演習が実行されていません。");return;

                }
                //              システムユーザ以外または汎用アクションでないならすぐにリプライ送信
                if(!to.isSystemUser(this.phase) || !action.is(CardData.Types.action)){
                        NotificationMessage msg  = new NotificationMessage(this.pid, action, to, from, cc, reply);
                        executeAction(msg);
                        return;
                }

                if(action.abortaction  != null && action.abortaction.length() != 0){
                        int c = countQueuedAction(new String[]{action.abortaction});
                        if(c ==  0){
                                rejectAction(action, from, to, cc, reply, "アクションが実行中でないため、中止することはできません。");
                                return;
                        }
                }

                Collection<ReplyData> rep = ReplyData.findReply(this.phase, action.id, to, action.attachments);
                if(rep.isEmpty()){
                        logger.warn("アクションに対する応答が見つかりません。" + action.toString());
                }
                acceptAction(action, from, to, cc,reply);
        }
        /**受け付けたアクションをキューに入れ、受付通知を発行*/
        protected void enqueueAction(NotificationMessage msg){

                history.add(msg);
                actionQueue.add(msg);
                logger.info("action queued." +  msg.toString());
                Notifier.dispatch(msg);
        }
        /**受け付けたアクションを即時実行*/
        protected void executeAction(NotificationMessage msg){
                msg.replyDate = new Date();//TODO:必要?
                history.add(msg);
                updateSystemState(msg.action, msg.id);
                Notifier.dispatch(msg);
                logger.info("action executed." +  msg.toString());
        }
        /**操業状態を更新*/
        protected void updateOperationState(){
                for(OperationStateDef od : operationStateDef){
                        if(this.phase != od.phase)      continue;

                        OperationLevelDef lv = od.getOperationLevel(this.phase, this.start);
                        if(lv != null && this.operationlevel != lv.level){
                                logger.info("operation state changed:"  + String.valueOf(this.operationlevel) + " to "+ String.valueOf(lv.level) + ":: "+this.toString());
                                this.operationlevel = lv.level;
                                onChangeOperationState();
                        }
                }
        }
        /***/
        public void onChangeOperationState(){
                String message = "操業レベルが変化しました。現在のレベル:"+String.valueOf(this.operationlevel);
        //      postTeamNotification(message, NotificationMessage.LEVEL_HIDDEN, null);
        }
        /**システムからの通知を送信します。*/
        public void postTeamNotification(String message, int level, String[] state){
                CardData data = CardData.find(CardData.Types.notification);
                NotificationMessage msg = constructNotification(this.pid, data,  Member.TEAM, Member.SYSTEM, null, null);
                msg.team = team;
                msg.message = message;
                msg.action.statecards = state;
                msg.level = level;
                logger.info("システムからの通知メッセージを送信:" + msg.toString());
                Notifier.dispatch(msg);
                this.addHistory(msg);

        }
        /**リプライの内容に従ってシステムステートを追加または削除する*/
        protected void updateSystemState(final ReplyData rep, final String msgid){

                if(rep.addstate != null){
                        for(String cur : rep.addstate){
                                addState(cur, new Date(), msgid);
                        }
                }
                if(rep.removestate != null){
                        for(String cur : rep.removestate){
                                removeState(cur);
                        }
                }
        }
        /**アクションの内容に従ってシステムステートを追加または削除する*/
        protected void updateSystemState(final CardData action, final String msgid){
                if(action.addstate != null){
                        for(String cur : action.addstate){
                                addState(cur, new Date(), msgid);
                        }
                }
                if(action.removestate != null){
                        for(String cur : action.removestate){
                                removeState(cur);
                        }
                }
        }

        /**自動アクション履歴(アクションID、発火日時)*/
        public HashMap<String, Date> autoActionHistory =  new HashMap<String, Date>();

        /***自動アクションを発火する*/
        public synchronized void processTriggerAction(){
                CardData[] auto = CardData.findList(CardData.Types.auto);
                for(CardData cur : auto){
                        if(autoActionHistory.containsKey(cur.id))continue;
                        Member from = Member.getMemberByRole(this.team, cur.from == null ? Member.SYSTEM.role: cur.from);
                        Member to = Member.getMemberByRole(this.team, cur.to == null ? Member.TEAM.role  : cur.to);

                        if(from == null){
                                logger.error("自動アクションのfromに指定されたユーザが定義されていません。"+ cur.toString());
                                return;
                        }
                        if(to == null){
                                logger.error("自動アクションのtoに指定されたユーザが定義されていません。"+ cur.toString());
                                return;
                        }

                        if(!evaluateAutoAction(cur))
                                continue;
                        if(!autoActionHistory.containsKey(cur.id)){
                                logger.info("自動アクションを受付:" + cur.name +  ":" + (cur.delay / 1000)+"秒後");

                                List<Member> cc = Member.getMembers(this.team, cur.cc);
                                registerTrigger(cur, to, from, cc);
                        }
                }
        }
        /**自動アクションをスケジュール登録する*/
        protected void registerTrigger(CardData cur, Member to, Member from, List<Member> cc) {

                TimerTask handler = new TimerTask(){
                        @Override
                        public synchronized  void run() {
                                if(!autoActionHistory.containsKey(cur.id)){
                                        logger.info("自動アクションを実行:" + cur.name);
                                        acceptAction(cur,  from, to, cc.toArray(new Member[cc.size()]), null);
                                        autoActionHistory.put(cur.id, new Date());
                                }
                        }
                };
                Timer tm = new Timer();tm.schedule(handler, cur.delay*1000);

                this.autoActionTimers.add(tm);
        }
        protected void clearAutoActionTimers() {
                for (Timer t: this.autoActionTimers) {
                        t.cancel();
                }
                this.autoActionTimers.clear();
        }

        ArrayList<Timer> autoActionTimers = new ArrayList<Timer>();
        /**ステート条件の演算子*/
        public static enum Operator{
                AND,
                NAND,
                OR,
                NOR,
                NOT
        };

        /**システムステートを拡張条件式で評価。
         * 整数文字列はステート番号として解釈する。
         * 次の文字列は論理演算子として解釈する。
         * AND
         * NOT
         * OR
         * NAND
         * NOR
         * 論理演算子はいずれか1つだけ定義できる。どれも定義されなければ既定の動作となる。
         * NOTはオペランドをひとつだけ受け付ける。その他は1つ以上のオペランドを受け付ける(オペランドが1つしかない場合は、常に真(演算子なし)または常に偽(NOT)になる)。
         * (NAND/NORでも代用できるが、簡単のため実装)
         * 条件が空の場合は条件なしに真を返す
         * */
        public static Operator[] OperatorList = new Operator[]{Operator.AND,Operator.NAND, Operator.OR,Operator.NOR,Operator.NOT};

        /**statecondition系プロパティの解析<br>
         * 演算子とステート番号の配列に分解する
         * */
        protected Pair<Operator, String[]> extractStateNumbers(final String[] cond){
                try{
                        if(cond == null)return new Pair<Operator, String[]>(Operator.OR, new String[0]);
                        Collection<String> states = new ArrayList<>();
                        Operator operator = null;
                        for(String i : cond){
                                if(Util.contains(OperatorList, i)){
                                        if(operator != null) throw new WorkflowException("演算子を複数回使用することはできません。");
                                        operator = Operator.valueOf(i);
                                }else{
                                        if(i == null || i.length() == 0)continue;
                                        states.add(i);
                                }
                        }
                        return new Pair<Operator, String[]>(operator, states.toArray(new String[states.size()]));
                }catch(Throwable t){
                        throw new WorkflowException("ステート条件の書式が不正です。" + Util.toString(cond), t);
                }
        }

        public String[] SplitElm(String str0){
            String str1,str2,str3;
            int flag=0;
            List<String> buf = new ArrayList<String>();
            int P0 = str0.indexOf(",");
            int P1 = str0.indexOf("(");
        int P2 = str0.lastIndexOf(")");
        while(P0>0) {
                if(P0<P1) {
                        buf.add(str0.substring(0,P0));
                        str0 = str0.substring(P0+1);
                        flag = 0;
                        P0=str0.indexOf(",");
                        P1=str0.indexOf("(");
                                P2= str0.indexOf(")");
                }else {
                        str1=null;
                        if(P1>=0) {
                           str1 = str0.substring(0,P1+1);
                           str0 = str0.substring(P1+1);
                        }
                        P0 = str0.indexOf(",");
                        P1 = str0.indexOf("(");
                        P2 = str0.indexOf(")");
                        if (P1<0&&P2<0) {
                                buf.add(str0.substring(0,P0));
                                str0 = str0.substring(P0+1);
                                P0 = str0.indexOf(",");
                                P1= str0.indexOf("(");
                                        P2= str0.indexOf(")");
                        }else if(P1<P2) {
                                str1 = str1+str0.substring(0,P1+1);
                                str2 = str0.substring(P1+1,P2+1);
                                str3 = str0.substring(P2+1);
                                //if(P1>=0) {
                                                flag+=1;
                                //}
                                while(flag>0) {
                                        P1 = str2.indexOf("(");
                                        if(P1>=0) {
                                                str1 = str1 + str2.substring(0,P1+1);
                                                P2 = str3.indexOf(")");
                                                str2=str2.substring(P1+1)+str3.substring(0,P2+1);
                                                str3 = str3.substring(P2+1);
                                        }else {
                                                flag = 0;
                                                buf.add(str1+str2);
                                                P0 = str3.indexOf(",");
                                                if(P0 == 0) {
                                                        str0 = str3.substring(P0+1);
                                                        P0 = str0.indexOf(",");

                                                }else {
                                                        str0 = null;
                                                        P0 =-1;
                                                }
                                                P1= str0.indexOf("(");
                                                P2= str0.indexOf(")");
                                        }
                                }
                        }else {
                                buf.add(str1+str0.substring(0,P2+1));
                                str3 = str0.substring(P2+1);
                                P0 = str3.indexOf(",");
                                        if(P0 == 0) {
                                                str0 = str3.substring(P0+1);
                                                P0 = str0.indexOf(",");
                                        }else {
                                                str0 = null;
                                                P0 =-1;
                                        }
                                        P1= str0.indexOf("(");
                                        P2= str0.indexOf(")");
                        }
                }
         }
        buf.add(str0);
        return buf.toArray(new String[buf.size()]);
        }
        public boolean memberEvalEQ(String userid,String state){
            String buf[];
            String logic;
            boolean result,elm;
            String str0;
            int ic;
            result = true;
            str0=state.trim();  //すべての空白を除く
            str0=str0.toUpperCase(); //すべて大文字に変換する

            int P1=str0.indexOf("(");
            logic=str0.substring(0,P1-1);
            int P2=str0.lastIndexOf(")");
            str0=str0.substring(P1+1,P2-1);

            buf=SplitElm(str0);

            ic=0;
            switch (logic){
            case "AND":
            case "NOR":
            case "NOT":
                result=true;
                break;
            case "OR":
            case "NAND":
                result=false;
                break;
            default:
                System.out.println("Logic Error "+logic);
            }
            while (buf[ic]!=null) {
                if (buf[ic].indexOf("(")>=0){
                    elm=memberEvalEQ(userid,buf[ic]);
                }else{
                    elm=memberHasState(userid,buf[ic]);
                }
                switch (logic){
                case "AND":
                    result=elm && result;
                    break;
                case "NOR":
                    result= !(elm) && result;
                    break;
                case "NOT":
                    result= !(elm) && result;
                    break;
                case "OR":
                    result= elm || result;
                    break;
                case "NAND":
                    result= !(elm) || result;
                    break;

                }
                ic+=1;
            }
            return result;
        }
        /**ステート条件を評価する
         * @return 条件にヒットしたかどうか
         * */
        protected  boolean evaluateMemberStateCondition(final String userid, final String cond[], final Operator defaultOperation){
                boolean fl=false;
                if(cond != null) {
                if(cond.length == 1) {
                        if(cond[0].indexOf("(")>=0) {
                                fl=true;
                        }
                        }
                }
            if (fl) {
                return memberEvalEQ (userid, cond[0]) ;
            }else {
                        Pair<Operator, String[]> p = extractStateNumbers(cond);
                        if(p.leader == null) p.leader = defaultOperation;
                        boolean and = p.leader == Operator.AND || p.leader == Operator.NAND;
                        boolean not = p.leader == Operator.NAND || p.leader == Operator.NOR || p.leader == Operator.NOT;
                        boolean result = and ? memberHasAllStates(userid, p.trailer) : memberHasStates(userid, p.trailer);
                        boolean ret =  (not ? !result : result);
                        if(ret){
                                logger.info("ユーザステート条件がヒットしました: " + (and ? " and " : " or ") + (not ? " not " : " ") + Util.toString(p.trailer) + ":" + ret);
                        }
                    return ret;
                }
        }

        /**メンバがいずれかのステートを所有しているか。(OR)*/
        protected boolean memberHasStates(final String userid, final String[] states){
                if(states == null)return true;
                for(String s : states){
                        if(memberHasState(userid,s))
                                return true;
                }
                return false;
        }
        /**メンバがすべてのステートを所有しているか。(AND)*/
        protected boolean memberHasAllStates(final String userid, final String[] states){
                if(states == null)return true;
                for(String s : states){
                        if(!memberHasState(userid, s))
                                return false;
                }
                return true;
        }
        /**メンバが指定されたステートを所有しているか。stateがnullならtrue*/
        protected boolean memberHasState(final String userid, final String state){
                if(state == null)return true;
                Set<String> list = this.memberStates.get(userid);
                if(list == null)return false;
                return list.contains(state);
        }

        public boolean EvalEQ(String state){
            boolean result = true;
            boolean elm = false;
            String str0 = state;
            int P1=str0.indexOf("(");
            String logic=str0.substring(0,P1);
            int P2=str0.lastIndexOf(")");
            str0=str0.substring(P1+1,P2);

            String[] buf=SplitElm(str0);

            int ic=0;
            switch (logic){
            case "AND":
            case "NOR":
            case "NOT":
                result=true;
                break;
            case "OR":
            case "NAND":
                result=false;
                break;
            default:
                System.out.println("Logic Error "+logic);
            }
            while (ic < buf.length && buf[ic]!=null) {//2020/10/7 仮対処:　bufにnull要素がない場合にArrayIndexOutOfBoundsException
                if (buf[ic].indexOf("(")>=0){
                    elm=EvalEQ(buf[ic]);
                }else{
                    elm=hasState(buf[ic]);
                }
                switch (logic){
                case "AND":
                    result=elm && result;
                    break;
                case "NOR":
                    result= !(elm) && result;
                    break;
                case "NOT":
                    result= !(elm) && result;
                    break;
                case "OR":
                    result= elm || result;
                    break;
                case "NAND":
                    result= !(elm) || result;
                    break;

                }
                ic+=1;
            }
            return result;
        }

        /**ステート条件を評価する*/
protected boolean evaluateStateCondition(final String cond[], final Operator defaultOperation) {
        boolean fl=false;
        if(cond != null) {
                if(cond.length == 1) {
                        if(cond[0].indexOf("(")>=0) {
                                fl=true;
                        }
                        }
        }
        if (fl) {
                return EvalEQ (cond[0]) ;       
        }else {         
                        Pair<Operator, String[]> p = extractStateNumbers(cond);
                        if(p.leader == null) p.leader = defaultOperation;
        //              if(p.trailer == null || p.trailer.length == 0) return true;
                        boolean and = p.leader == Operator.AND || p.leader == Operator.NAND;
                        boolean not = p.leader == Operator.NAND || p.leader == Operator.NOR || p.leader == Operator.NOT;

                        boolean result = and ? hasAllStates(p.trailer) : hasStates(p.trailer);

                        boolean ret =  (not ? !result : result);
                        if(ret){
                                logger.info("ステート条件がヒットしました: " + (and ? " and " : " or ") + (not ? " not " : " ") + Util.toString(p.trailer) + ":" + ret);
                        }
                        return ret;
                }
          }

        /**自動アクションのステート条件を評価
         *
         * */
        protected boolean evaluateAutoAction(final CardData action){
                boolean hasStates = evaluateMemberStateCondition(action.from, action.statecondition, Operator.AND);
                if(hasStates){
                        return true;
                }

                if(action.systemstatecondition == null || action.systemstatecondition.length == 0)
                        return true;
                return evaluateStateCondition(action.systemstatecondition, Operator.OR);

        }
        /***フェーズ開始からの経過時間による(アクションのない)イベントを発火する*/
        public synchronized /*ここでConcurrentModificationExceptionが上がる*/void processTriggerEvent(){
                try{
                        CardData data = CardData.find(CardData.Types.notification);
                        Collection <ReplyData> replies = ReplyData.loadReply(this.phase);

                        for(Iterator<ReplyData> i = replies.iterator(); i.hasNext();){
                                final ReplyData r = i.next();
                                if(!r.isTriggerAction()) continue;
                                if(r.elapsed == -1)     continue;

                                Date notbefore = new Date(this.start.getTime() + r.elapsed * 1000);
                                if(!new Date().after(notbefore)){
                                        logger.debug(String.format("トリガーイベントは待機中です。宛先:%s, 名前:%s, メッセージ:%s,予定時刻:%s",r.to,r.name,r.message, new SimpleDateFormat("MM/dd HH:mm:ss").format(notbefore)));
                                        continue;
                                }

                                //すでに発火済ならスキップ
                                if((r.state != null && r.state.length() != 0) &&  triggerIsFired(/*Integer.valueOf(r.state)*/r.state, r.to)){//, r.to))
                                        continue;
                                }

                                if(!evaluateStateCondition(r.statecondition, Operator.OR))
                                        continue;

                                logger.info(String.format("トリガーイベントを送信します。宛先:%s@%s, 名前:%s, メッセージ:%s",r.to, this.team, r.name, r.message));
                                //アクションデータをクローンしている。こうしないとアクションキューから取り出したインスタンスを書き換えたときにおかしなことになる。
                                CardData data2 = new ObjectMapper().readValue(data.toString(), CardData.class);
                                Member to = Member.roleToMember(this.team, r.to);
                                Member from = Member.roleToMember(this.team, r.from);
                                NotificationMessage msg = constructNotification(this.pid, data2,  to, from, null, null);
                                msg.message = r.message;
                                msg.team = this.team;
                                msg.sentDate = new Date();

                                String[] statecards = null;

                                if(msg.action.statecards != null && msg.action.statecards.length != 0){
                                        statecards = Util.join(statecards, msg.action.statecards);
                                }

                                if(r.state != null && r.state.length() != 0){
                                        statecards = Util.put(statecards, r.state);
                                }else{
                                        logger.warn("トリガーイベントにステートカードが設定されていません。このイベントは破棄されます。" + r.toString());
                                        continue;
                                }

                                msg.action.statecards = statecards;
                                msg.message = r.message != null ? r.message : "";
                                updateSystemState(r, msg.id);

                                fireTrigger(msg);

                                Notifier.dispatch(msg);
                                this.addHistory(msg);

                                logger.info(String.format("トリガーイベントを送信しました。宛先:%s, 名前:%s, メッセージ:%s",r.to,r.name, r.message));

                        }
                }catch(Throwable t ){
                        logger.error("トリガーイベント通知に失敗しました。", t);
                }

        }
        /**トリガーイベントを発火する*/
        protected void fireTrigger(NotificationMessage msg){
                if(msg == null || msg.action == null || msg.action.statecards == null || msg.action.statecards.length == 0){
                        logger.error("トリガーにステートカードがありません。" + msg != null ? msg.toString() : "null");
                }
                for(String state : msg.action.statecards){
                        if(msg.to == null || msg.to.role == null){
                                logger.warn("トリガーイベントにtoが指定されていません。イベントは無視されます。" +String.valueOf(msg.toString()));
                                continue;
                        }
                        TriggerEvent ev  =findTriggerEvent(state, msg.to.role);
                        if(ev != null){
                                logger.warn("送信済のトリガーイベントを送信しようとしました。イベントは無視されます。ステート:" +String.valueOf(state));
                                continue;
                        }
                        TriggerEvent newEvent = new TriggerEvent(state, msg.from != null ? msg.from.role : null, msg.to != null ? msg.to.role : null);
                        logger.info(String.format("トリガーイベントを履歴に保存しました。メッセージ:%s, from:%s,to:%s, ステート:%s",msg.message, newEvent.from, newEvent.to, newEvent.state));
                        logger.info(String.valueOf(this.triggerEvents.size())+ "のトリガーイベントが履歴にあります。");


                        this.triggerEvents.add(newEvent);
                }
                if(msg.reply != null){
                        updateSystemState(msg.reply, msg.id);
                }
        }

        protected TriggerEvent findTriggerEvent(String state, String to){
                for(TriggerEvent e : this.triggerEvents){
                        if(e.state == state && e.to.equals(to))
                                return e;
                }
                return null;
        }
        public static  NotificationMessage constructNotification(long pid, CardData data, Member to, Member from, Member[] cc, NotificationMessage replyTo) throws WorkflowException{
                if(data.is(CardData.Types.talk)){
                        //送信者と受信者にディスパッチ
                }else if(data.is(CardData.Types.notification)){
                        //送信者と受信者にディスパッチ
                }else if(data.is(CardData.Types.action)){
                        //送信者と受信者にディスパッチ
//              }else if(data.type == "response"){//TODO:いらんな
//                      //送信者と受信者にディスパッチ
//                      if(replyTo == null)throw new WorkflowException("問い合わせ先が指定されていません。", HttpServletResponse.SC_BAD_REQUEST );
//              }else if(data.type == "notification"){
//                     
                }
                NotificationMessage msg = new NotificationMessage(pid, data, to, from, cc, replyTo);
                return msg;
        }
        /**自動アクション、トリガーイベントなどの発生条件をスキャンし必要に応じて起動する
         * */
        class Worker extends TimerTask{
                protected Timer timer = new Timer();
                int interval = Integer.parseInt(System.getProperty("check.interval","1000"));
                public Worker(int interval){this.interval = interval;}
                public Worker(){}
                public void start(){
                        this.timer.schedule(this, this.interval, this.interval);
                        logger.info(String.format("worker started.team=%s, phase=%d", WorkflowInstance.this.team, WorkflowInstance.this.phase));
                }
                public void stop(){
                        this.timer.cancel();
                        logger.info("worker stoped.");
                }

                /**定期的に実行される処理**/
                @Override
                public void run() {
                        //経過時間に応じて操業レベル変化
                        if(!WorkflowInstance.this.isRunning()){
                                return;
                        }
                        updateOperationState();
                        try{
                                //自動アクションを検査
                                processTriggerAction();
                                //トリガーイベントを検査
                                processTriggerEvent();
                                //キューを検査してディスパッチ
                                dispatchAction();
                        }catch(WorkflowException t){
                                logger.error("failed to dispatch action.", t);
                        }catch(Throwable t){
                                logger.error("failed to dispatch action.", t);
                        }
                }
        }

        /**履歴からアクション実行履歴を探索*/
        protected Collection<NotificationMessage> getActionHistory(String actionid){
                Collection<NotificationMessage> ret = new ArrayList<>();
                for(NotificationMessage n : history){
                        if(n.action.id.equals(actionid)){
                                ret.add(n);
                        }
                }
                return ret;
        }
        /**キューからアクション実行要求を探索*/
        protected Collection<NotificationMessage> getActionQueue(String actionid, int order){
                Collection<NotificationMessage> ret = new ArrayList<>();
                for(NotificationMessage n : actionQueue){
                        if(n.action.id.equals(actionid) && n.action.curretorder == order){
                                ret.add(n);
                        }
                }
                return ret;
        }

        protected boolean checkStateCondition(NotificationMessage request, ReplyData rep){
                return evaluateStateCondition(rep.statecondition, Operator.OR);
        }
        /**アクションに対するリプライが実行条件を満たすか確認
         * @param request アクション要求イベント
         * @param rep リプライ
         * */
        protected boolean checkReply(NotificationMessage request, ReplyData rep){

                if(!checkStateCondition(request, rep)){
                        logger.info(rep.name + ":システムステート条件(statecondition)を満たしていません。");
                        return false;
                }
                if(rep.cardcondition != null){

                        for(String id : rep.cardcondition){
                                if(getElapsedTime(id)<0){
                                        logger.info(rep.name + ":実行条件(cardcondition)を満たしていません。");

                                        return false;
                                }
                        }
                }
                if(!rep.checkTimeCondition((act)->getElapsedTime(act)))
                        return false;

                //リソース制約を判定
                if(rep.constraints != null){
                        try{
                                Collection<String> actions = (Collection<String>)rep.constraints.get("actionid");
                                Integer multiplicity = (Integer)rep.constraints.get("multiplicity");
                                if(actions != null && multiplicity != null){
                                        int c = countQueuedAction(actions.toArray(new String[actions.size()]));
                                        int m = multiplicity.intValue();
                                        if(c <=m){
                                                logger.log(String.format(rep.name + ":多重度条件(constraints)を満たしていません。キューの数:%d, 上限:%d, リプライ:%s", c, m ,rep.toString()));
                                                return false;
                                        }
                                }
                        }catch(Throwable t){
                                logger.error(rep.name + ":リソース制約条件の解析に失敗しました。リプライ定義を確認してください。." + rep.toString(), t);
                        }
                }

                logger.info("リプライを確定しました。" + rep.name + ":"+rep.toString());
                return true;
        }
        /**リプライの決定処理で使用するコールバックインタフェース*/
        public interface Validatable<T1, T2> {
                /**
                 * @param t1 チェック処理に必要なパラメタのキー
                 * @return チェック処理に必要なパラメタの値
                 * */
                public T2 get(T1 t1);
        }
        /**アクションキュー内にある指定されたIDのアクションの数を返す*/
        protected int countQueuedAction(final String actionIds[]){
                if(actionIds == null || actionIds.length == 0)return 0;
                int ret = 0;
                for(Iterator<NotificationMessage> i  = actionQueue.iterator(); i.hasNext();){
                        NotificationMessage m = i.next();
                        for(String id : actionIds){
                                if(m.action.id.equals(id))      ret ++;
                        }
                }
                return ret;
        }

        /**指定されたステートカードのトリガが発火済かを検査**/
        protected boolean triggerIsFired(final String state, final String to){//, final String role){
                for(TriggerEvent ev : this.triggerEvents){
                        if(ev.state != null && ev.state.equals(state) && ev.to.equals(to)){
                                return true;
                        }
                }
                return false;

        }
        /**履歴からアクションが実行されたあとの経過秒数を返す。実行されていなければ-1**/
        protected long getElapsedTime(final String actionId){
                if(actionId == null)return 0;
                for(Iterator<NotificationMessage> i = history.iterator(); i.hasNext();){
                        NotificationMessage m = i.next();

                        if(!m.action.id.equals(actionId))
                                continue;

                        logger.info("履歴にアクションがあります。" + m.action.name);
                        if(m.replyTo != null && m.replyDate == null)
                                throw new RuntimeException("バグ: 履歴にreplyDateが設定されていない");
                        else if(m.replyDate != null){
                                long ret = ((new Date().getTime() - m.replyDate.getTime())/1000);
                                logger.info(String.format("アクション[%s]は%d秒前に実行されました。",  m.action.name, ret));
                                return ret;
                        }else if(m.sentDate != null){
                                long ret = ((new Date().getTime() - m.sentDate.getTime())/1000);
                                logger.info(String.format("アクション[%s]は%d秒前に送信されました。",  m.action.name, ret));
                                return ret;
                        }else{
                                throw new RuntimeException("バグ: 履歴にreplyDateが設定されていない(2)");
                        }
                }
                logger.info(String.format("アクション[%s]はまだ実行されていません。", actionId));

                return -1;
        }
        /**{@link WorkflowInstance#getElapsedTime(String)}の配列バージョン**/
        protected Map<String, Long> getElapsedTimes(final String[] actions){
                Map<String, Long> ret = new HashMap<String, Long>();
                if(actions == null || actions.length == 0)return ret;
                for(String act : actions) {
                        ret.put(act, Long.valueOf(getElapsedTime(act)));
                }
                return ret;
        }


        /**アクション要求からリプライを作成
         *   fromとtoを入れ替え
         *    該当するリプライを設定
         *    replyDateに現在時刻を設定
         *   
         * */
        protected NotificationMessage makeReplyMessage(final NotificationMessage actionRequest){
                NotificationMessage reply = new NotificationMessage(this.pid, actionRequest.action,
                                actionRequest.from, actionRequest.to, actionRequest.cc, null);
                reply.sentDate = actionRequest.sentDate;//TODO:うーん？？
                int nextorder = 0;
                Collection<NotificationMessage> queued = getActionQueue(actionRequest.action.id, actionRequest.action.curretorder);
                if(queued != null && queued.size() != 0){
                        logger.debug("要求されたアクションは実行待ちです。オーダー:" +String.valueOf(actionRequest.action.curretorder));
                        nextorder = queued.iterator().next().action.curretorder;
                }
                Collection<ReplyData> replies = ReplyData.findReply(this.phase,  actionRequest.action.id, actionRequest.to, nextorder, 
                                actionRequest.action.attachments);//Util.toIntArray(actionRequest.action.attachments));
                if(replies.size() == 0){
                        logger.debug(String.format("リプライ候補が見つかりません。エラーリプライを返します。action=%s, phase=%s, to=%s",
                                        actionRequest.action.id, String.valueOf(phase), actionRequest.to.toString()));
                        reply.reply = ReplyData.getErrorReply(phase, null);
                        reply.replyDate = new Date();
                        return reply;
                }
                logger.debug(replies.size() + "個のリプライ候補がヒットしました。オーダー:" +String.valueOf(nextorder));


                ReplyData rep = null;

                //priorityでリプライ候補をソートする
                ReplyData[] arr=replies.toArray(new ReplyData[replies.size()]);
                List<ReplyData> list =Arrays.asList(arr);
                Collections.sort(list, new Comparator<ReplyData>() {
                        @Override
                        public int compare(ReplyData o1, ReplyData o2) {
                                if(o1.priority ==o2.priority) return 0;
                                else if(o1.priority <o2.priority) return 1;
                                else return -1;
                        }
                });

                for(ReplyData r : list){//TODO: 最初にヒットした応答を返す-
                        if(checkReply(actionRequest, r)){rep = r;break;}
                }

                //送信待ちのときは続行、そうでないときはエラー
                if(rep==null){
                        logger.warn(String.format("該当するリプライデータが見つかりません。action=%s, phase=%s, to=%s",
                                        actionRequest.action.id, String.valueOf(phase), actionRequest.to.toString()), null);
                        return null;
                }else{
                        reply.reply = rep;
                        logger.debug(String.format("該当するリプライデータが見つかりました。reply=%s, action=%s, phase=%s, to=%s : %s",
                                        reply.id, actionRequest.action.id, String.valueOf(phase), actionRequest.to.toString(), reply.reply.name));
                }
                reply.replyDate = new Date();
                return reply;

        }
        /**後続リプライがあるか*/
        protected boolean isLastAction(NotificationMessage msg){

                try{   
                        Collection<ReplyData> reps = ReplyData.findReply(phase,  msg.action.id, msg.from, msg.action.curretorder + 1,
                                        msg.action.attachments);//Util.toIntArray(msg.action.attachments));
                        boolean ret = (reps.size() == 0);
                        for(ReplyData d : reps.toArray(new ReplyData[reps.size()])){
                                if(ReplyData.Types.NOT_FOUND.equalsIgnoreCase(d.type)){
                                        logger.debug(String.format("アクション%s(%s)には後続リプライがありません(notfound)。", msg.action.id, msg.action.name));
                                        ret = true;break;//見つからないが候補にあったらもう無理でしょう、たぶん
                                }
                        }

                        //リプライ候補がエラーリプライしかないなら後続アクションなしで返す
                        if(!ret){
                                String desc = "";
                                for(Iterator<ReplyData> i = reps.iterator(); i.hasNext();){
                                        desc += i.next().name + ";";
                                }
                                logger.debug(String.format("アクション%s(%s)には後続リプライがあります。[%s]", msg.action.id, msg.action.name, desc));
                                return ret;
                        }else{
                                logger.debug(String.format("アクション%s(%s)には後続リプライがありません(空)。", msg.action.id, msg.action.name));
                                return ret;
                        }
                }catch(WorkflowException t){
                        logger.warn(String.format("アクション%s(%s)には後続リプライがありません。(エラー)", msg.action.id, msg.action.name),t);
                        return true;
                }
        }
        /**応答を送信
         *
         * キューにリプライが空のエントリがあれば
         *              応答を作成してキューに格納し、元のエントリは削除
         * キューにリプライが設定されたエントリがあれば、
         * リプライが空のエントリがあったらリプライを探索
         *
         * */
        protected synchronized void dispatchAction() throws WorkflowException{
                //キューを探索
                if(actionQueue.size() != 0){
                        logger.debug(String.format("チーム %s: %d個のアクションが待機中",team, actionQueue.size()));
                }

                for(NotificationMessage n: actionQueue){
                        try{
                                if(n.reply != null) {logger.warn("???? リプライ付きアクションがキューにある...",  null);}

                                String[] attachments = n.action.attachments;//Util.toIntArray(n.action.attachments);
                                Collection<ReplyData> candidates = ReplyData.findReply(this.phase,  n.action.id, n.to, n.action.curretorder, attachments);

                                //キューになければ受付完了通知を送信
                                NotificationMessage rep = null;

                                if(candidates == null || candidates.size() == 0){
                                        if(!n.to.isSystemUser(phase)){
                                                NotificationMessage err = new NotificationMessage(this.pid, n.action, n.from, n.to, n.cc, null);
                                                err.sentDate = n.sentDate;
                                                err.reply = ReplyData.getNullReply(phase);
                                                rep = err;
                                                logger.debug("自動応答ユーザでないため空のリプライを返します。" + n.toString());
                                        }else{
                                                NotificationMessage err = new NotificationMessage(this.pid, n.action, n.from, n.to, n.cc, null);
                                                err.sentDate = n.sentDate;
                                                err.reply = ReplyData.getErrorReply(phase, null);
                                                rep = err;
                                                logger.debug("リプライ候補がないためエラーリプライを返します。" + n.toString());
                                        }
                                }else{
                                        rep = makeReplyMessage(n);
                                        if(rep == null){
                                                logger.debug("リプライ候補の実行条件が満足されていないため保留します。" + n.toString());
                                                continue;
                                        }
                                }

                                //応答可能なら応答
                                if(!new Date().after(new Date(rep.sentDate.getTime() + rep.reply.delay*1000))){
//                                      Date sent = new Date(rep.sentDate.getTime());
                                        Date when = new Date(rep.sentDate.getTime() + rep.reply.delay*1000);
                                        logger.debug("遅延条件(delay)が満たされていないためリプライを保留します。[" + rep.reply.name + "],予定時刻:" + new SimpleDateFormat("MM/dd HH:mm:ss").format(when));
                                        continue;
                                }
                                rep.replyTo = n;
                                rep.replyDate = new Date();
                                rep.team = this.team;

                                updateSystemState(n.action, rep.id);


                                updateSystemState(rep.reply, rep.id);
                                Notifier.dispatch(rep);
                                //履歴に追加
                                addHistory(rep);
                                if(rep.reply.abort || isLastAction(rep)){
                                //アクションキューから削除
                                        actionQueue.remove(n);
                                        logger.info("キューからアクションを削除しました。"+n.action.name + ":"+toString());
                                }else{
                                        n.action.curretorder ++;
                                        logger.info("アクションに後続リプライがあるためキューに残します。"+ n.action.name + ":" + toString());
                                }
                                processAbortAction(rep);


                                logger.info(String.format("リプライを送信しました。%s", rep.toString()));
                        }catch (WorkflowException e) {
                                throw e;
                        }
                }

        }
        protected void processAbortAction(NotificationMessage msg){

                if(msg.action == null ||msg.action.abortaction == null || msg.action.abortaction.length() == 0)
                        return;
                for(NotificationMessage act : actionQueue){
                        if(msg.action.abortaction.equals(act.action.id)){
                                logger.info("アクションを中止しました。"+ act.action.name);
                                actionQueue.remove(act);
                                return;
                        }
                }
                logger.warn("中止対象アクションがキューにありません。" + msg.action.name);
        }
        /**イベント履歴にイベントを追加*/
        protected void addHistory(NotificationMessage m){
                m.replyDate = new Date();//送信
                history.add(m);

        }
        /**履歴から指定されたイベントを取得*/
        public NotificationMessage getEventHistory(final String id){
                if(id == null || this.history == null || this.history.isEmpty()) return null;
                for(NotificationMessage m : this.history){
                        if(m.id.equals(id))return m;
                }
                return null;
        }
        /**指定されたユーザのイベント履歴を取得*/
        public Collection<NotificationMessage> getUserEventHistory(final String role){
                Collection<NotificationMessage> ret = new ArrayList<>();
                if(role == null || this.history == null || this.history.isEmpty()) return null;
                for(NotificationMessage m : this.history){
                        if(m.to != null && m.to.matches(role)) ret.add(m);

                        if(m.cc != null){
                                for(Member mm : m.cc){
                                        if(mm.role.equals(role))ret.add(m);
                                }
                        }

                        if(m.from.role.equals(role))ret.add(m);
                }
                return ret;
        }
        /**システムステートを返す*/
        protected Collection<StateData>  getSystemStateData(){
                Collection<StateData> ret = new ArrayList<>();
                Collection<StateData> all = StateData.loadAll();

                for(Iterator<StateData> i = all.iterator(); i.hasNext();){
                        StateData cur = i.next();
                        Object d = systemState.get(String.valueOf(cur.id));
                        if(d != null){
                                if(d instanceof Date)
                                        cur.when = (Date)d;
                                else if(d instanceof Long)
                                        cur.when = new Date(((Long)d).longValue());
                        }
                        ret.add(cur);
                }
                return ret;
        }

        //for serialize
        public NotificationMessage[] getActionQueue(){
                return this.actionQueue.toArray(new NotificationMessage[this.actionQueue.size()]);
        }
        public NotificationMessage[] getHistory(){
                return this.history.toArray(new NotificationMessage[this.history.size()]);
        }
        public Map<String, Object> getSystemState(){
                return this.systemState;
        }
        public TriggerEvent[] getTriggerEvent(){
                return this.triggerEvents.toArray(new TriggerEvent[this.triggerEvents.size()]);
        }
        /**アクションキューのリストを置き換える*/
        public void setActionQueue(NotificationMessage[] m){
                this.actionQueue.clear();
                if(m == null || m.length == 0)return;
                for(NotificationMessage msg : m){
                        this.actionQueue.add(msg);
                }
        }
        /**イベント履歴のリストを置き換える*/
        public void setHistory(NotificationMessage[] m){
                this.history.clear();
                if(m == null || m.length == 0)return;
                for(NotificationMessage msg : m){
                        this.history.add(msg);
                }

        }
        /**システムステートを追加*/
        public void setSystemState(Map<String,Object> s){
                this.systemState.clear();
                if(s == null || s.size() == 0)return;
                for(String state: s.keySet()){
                        Object val = s.get(state);
                        this.systemState.put(state, val);
                }
        }
        public void setTriggerEvent(TriggerEvent[] e){
                this.triggerEvents.clear();
                if(e == null || e.length == 0)return;
                for(TriggerEvent t : e){
                        t.date = adjustDate(t.date);
                        this.triggerEvents.add(t);
                }
        }

        /**時刻情報を矯正*/
        protected NotificationMessage adjustDate(NotificationMessage org){
                if(org == null)return null;
                NotificationMessage ret = org.clone();
                ret.sentDate = adjustDate(ret.sentDate);
                ret.replyDate = adjustDate(ret.replyDate);
                if(ret.replyTo != null){
                        ret.replyTo = adjustDate(ret.replyTo);//再帰する
                }

                return ret;

        }
        /**this.savedから現在時刻の差分だけ時刻を補正する*/
        protected Date adjustDate(Date org){
                if(org == null)return null;
                Date ret = (Date)org.clone();
                if(this.saved == null){
                        logger.warn("saved date not avairable.");
                        this.saved = new Date();
                }
                long offset = new Date().getTime() - this.saved.getTime();//開始時刻じゃなくて

                ret.setTime(ret.getTime() + offset);
                return ret;
        }
        /**ワークフローの状態を保存のためにシリアライズする*/
        public String save(){
                try{
                        if(this.saved == null)
                                this.saved = new Date();
                        String ret = new ObjectMapper().writeValueAsString(this);
                        return ret;
                }catch(Throwable t){
                        logger.error("ワークフローインスタンスのシリアライズに失敗しました。", t);
                        return "{}";
                }
        }
        /**ワークフローの状態を復元する*/
        public static WorkflowInstance load(final String data) throws WorkflowException{
                try{
                                WorkflowInstance inst = new ObjectMapper().readValue(data, WorkflowInstance.class);
                                inst.shift();
                                //inst.saved = null;
                                inst.resume();
                        return inst;
                }catch(Throwable t){
                        logger.error("ワークフローデータのロードに失敗しました。", t);
                        throw new WorkflowException("データ形式が不正です。" + t.getMessage(), HttpServletResponse.SC_BAD_REQUEST, t);
                }
        }
        /**
         * 日時をシフトする
         * */
        protected void shiftDate(Date target, long diff){
                if(target == null)return;

                long val = target.getTime() + diff;
                target.setTime(val);
        }
        /**
         * ワークフローの日時をシフトする<br>
         * 保存されたワークフロー状態を復元したときに日時情報を補正するために使用する
         * */
        protected void shift() throws WorkflowException{
                long now = new Date().getTime();//今

                if(this.saved == null)
                        throw new WorkflowException("保存日時が設定されていません。");
                long saved = this.saved.getTime();

                for(String l : this.systemState.keySet()){
                        Object when = this.systemState.get(l);
                        if(when != null){
                                Long value = Long.valueOf(now - saved +((Long)when).longValue());
                                this.systemState.put(l, value);
                        }
                }
                long diff = now - saved;
                for(TriggerEvent e : this.triggerEvents){
                        shiftDate(e.date, diff);
                }
                for(NotificationMessage m : this.history){
                        shiftRecursive(m, diff);
                }
                for(NotificationMessage m : this.actionQueue){
                        shiftRecursive(m, diff);
                }
                shiftDate(this.start, diff);
        }
        /**イベント履歴の日時をシフトする<br>
         * イベントの返信元イベントに対してもシフト処理を行う
         * */
        protected void shiftRecursive(NotificationMessage m, long diff){
                if(m == null)return;
                shiftDate(m.sentDate, diff);
                shiftDate(m.replyDate, diff);
                if(m.reply != null){
                        shiftDate(m.reply.fireWhen, diff);
                }
                if(m.replyTo != null)
                        shiftRecursive(m.replyTo, diff);
        }

        /**ステートカードが追加されたときの処理<br>
         * 現在の実装ではポイントカードを評価する
         * */
        protected void onAddState(final String state, final String msgid){
                evaluateScore(state, msgid);
        }
        /**ステートカードが削除されたときの処理<br>
         * 現在の実装では何もしない
         * */
        protected void onRemoveState(final String state){
                //
        }
        /**発行されたポイントカードのIDと、発行の引き金になったリプライIDのマップ*///発行日時がいいのか
        protected HashMap<String, Collection<String>> pointHistory = new HashMap<>();

        /**ポイントカードを評価し、ポイントカード取得履歴を更新する*/
        protected synchronized void evaluateScore(final String state, final String msgid){
                PointCard[] points = PointCard.list(this.phase);
                for(PointCard  cur : points){
                        if(checkPointcards(state, cur)){
                                //付与
                                this.score += cur.point;
                                Collection<String> replies = this.pointHistory.get(cur.id);
                                if(replies == null){this.pointHistory.put(cur.id, replies = new ArrayList<String>());}
                                replies.add(msgid);
                                if(!this.pointchest.contains(msgid)){this.pointchest.put(msgid,new ArrayList<PointCard>());}
                                this.pointchest.get(msgid).add(cur);
                                logger.info("ポイントカードを取得:" + this.team + ";" + cur.toString());
                        }
                }
        }
        /**ポイントカード取得時の追加処理。現在の実装ではNOP
         * */
        protected void onGetPointCard(final PointCard point, final ReplyData src){}
        /**ポイントカード多重度をチェック。パスしたらtrue。*/
        protected synchronized boolean checkPointcards(final String state, final PointCard pt){
                if(!this.isRunning())return false;

                if(pt.state!= null && !pt.state.equals(state)){
                        return false;
                }
                logger.info("システムステート" + state.toString() + "に対するポイントカードを評価中");
                //多重度チェック

                Collection<String> hist = pointHistory.get(pt.id);
                if(hist != null && !hist.isEmpty() && hist.size() >= pt.multiplicity){
                        logger.log(pt.name  + ":ポイント多重度超過:" + String.valueOf(pt.multiplicity));
                        return false;
                }

                //期間チェック
                if(this.start == null)return false;
                Date now = new Date();
                if(pt.before != 0 && (this.start.getTime() + pt.before*1000 > now.getTime())){
                        logger.log(pt.name + ":ポイント期限切れ:" + pt.toString());
                        return false;
                }
                if(pt.after != 0&& (this.start.getTime() + pt.after*1000 < now.getTime())){
                        logger.log(pt.name + ":ポイント期間未達:" + pt.toString());
                        return false;
                }

                if(pt.statecondition != null){
                        if(!evaluateStateCondition(pt.statecondition,Operator.AND))
                                return false;
                }
                logger.log("ポイント獲得条件をパス:" + this.team + ";" + pt.toString());
                return true;
        }

}
