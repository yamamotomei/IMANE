# reply.json:リプライカードのセット

アクションまたはトリガーに対する応答を定義します。

### Example

```json
{
  "states": [
    {
      "reply": [
        {
          "statecondition": [
            {}
          ],
          "addstate": [
            {}
          ],
          "removestate": [
            {}
          ],
          "delay": 10
        }
      ]
    }
  ],
  "message": [
    {}
  ]
}
```


### Documentation

### `.version`

**バージョン情報**

バージョン情報を記述します。

### `.states`

**リプライリスト**

フェーズとリプライのセットを定義します。

*型*: array

*最小項目*: 1

### `.states[]`

### `.states[].phase`

**フェーズ**

フェーズ番号を定義します。

*型*: string

*最小の長さ*: 1

### `.states[].reply`

**リプライのセット**

phaseで指定したフェーズで使用するリプライの配列を設定します。

*型*: array

*最小項目*: 1

### `.states[].reply[]`

### `.states[].reply[].actionid`

**アクションID**

このリプライの応答先となるアクションのIDを指定します。トリガーイベントを定義する場合は's0000'を指定します。必須

*型*: string

*最小の長さ*: 1

### `.states[].reply[].to`

**宛先ロールID**

このリプライの応答先となるユーザのロールIDを指定します。すべてのユーザを指定する場合はallを指定します。必須

*型*: string

*最小の長さ*: 1

### `.states[].reply[].from`

**送信元ロール**

トリガイベントを発行するときに送信元となるロールIDを指定します。定義済みロールのほか、systemを使用します。

*型*: string

### `.states[].reply[].hidden`

**非表示**

trueを指定すると、演習中にこのリプライをイベント一覧に表示しません。シナリオ制御のためのダミーのリプライを定義するときに使用します。省略値:false(表示する)

*型*: boolean

### `.states[].reply[].name`

**名前**

短い表示名を指定します。

*型*: string

### `.states[].reply[].state`

**添付ステートカード**

リプライに添付するステートカードのIDを指定します。トリガーアクションに対しては必須です。省略値:なし

*型*: string

*最小の長さ*: 1

### `.states[].reply[].statecondition`

**ステート条件**

ここで指定したシステムステートが登録されているときにリプライが有効になります。省略値:なし

*型*: array

### `.states[].reply[].statecondition[]`

*型*: string

### `.states[].reply[].addstate`

**追加システムステート**

リプライを発行するときに、ここで指定したシステムステートを追加します。省略値:なし

*型*: array

### `.states[].reply[].addstate[]`

*型*: string

### `.states[].reply[].removestate`

**削除システムステート**

リプライを発行するときに、ここで指定したシステムステートを削除します。省略値:なし

*型*: array

### `.states[].reply[].removestate[]`

*型*: string

### `.states[].reply[].message`

**メッセージ**

ユーザに通知する表示用の応答メッセージを記述します。省略値:なし

*型*: string

### `.states[].reply[].constraints`

**非推奨**

### `.states[].reply[].timecondition`

**非推奨**

### `.states[].reply[].order`

**評価順**

n-way型シーケンスで使用する、リプライの評価順を指定します。同じorderを持つリプライをグループ化し、orderの順に評価し、ヒットしたらリプライを返して次のorderをもつ評価対象のグループを評価します。省略値:1

*型*: number

### `.states[].reply[].delay`

**応答遅延**

アクションを受け付けてからリプライを返すまでの秒数を指定します。省略値は0(アクションに対して即座に応答する)です。

*型*: number

### `.states[].reply[].elapsed`

**経過時間**

トリガーイベントに使用します。フェーズ開始からの経過時間(秒数)を指定します。省略値:0

*型*: number

### `.states[].reply[].type`

**種類**

リプライの種類を指定します。'hidden'を指定すると、イベント一覧への表示を抑制します。'null'をは予約語です。省略値:なし

*型*: string

*許される値*: `` `hidden` `null`

### `.message`

*型*: array

*最小項目*: 1

### `.message[]`

### `.message[].id`

*型*: string

*最小の長さ*: 1

### `.message[].text`

*型*: string

*最小の長さ*: 1