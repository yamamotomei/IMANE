# actions.json:アクションカードのセット

アクションカードのセットを定義します。
アクションカードは、参加者または自動応答ユーザが自発的に開始することのできる行動を表現します。
アクションにはtypeプロパティで指定される次の種類があります。
- share: インシデント情報(ステートカード)を他のユーザに伝達することを目的としたアクション。すべてのシナリオに共通。
- notification: システムからの参加者へのメッセージ通知のために使用されるアクション。すべてのシナリオに共通。
- talk: ユーザ間の任意の会話に使用されるアクション。すべてのシナリオに共通。
- action: 特定のロールを持つユーザが実行できるアクション。シナリオごとに定義する。
- auto: 自動応答ユーザまたはシステムが演習の進行に応じて実行するアクション。シナリオ毎に定義する。

### サンプル

```json
{
  "actions": [
    {
      "type": "action",
      "roles": [
        "all"
      ],
      "attachments": [
        {}
      ],
      "phase": [
        {}
      ],
      "addstate": [
        {}
      ],
      "removestate": [
        {}
      ],
      "statecondition": [
        {}
      ],
      "timecondition": [
        {}
      ],
      "systemstatecondition": [
        {}
      ]
    }
  ]
}
```


### ドキュメント

### `.version`

**バージョン情報**

このデータのバージョン情報を記述します。

### `.actions`

**アクションカード**

アクションカードを定義します。

*型*: array

*最小項目*: 1

### `.actions[]`

### `.actions[].name`

**アクションの表示名**

アクションカードの表示名を設定します。この名前はイベント一覧に表示されます。

*型*: string

*最小の長さ*: 1

### `.actions[].type`

**アクションの種類**

アクションの種類を指定します。action,share,notification,talk,autoが使用可能です。

*型*: string

*許される値*: `action` `share` `notification` `talk` `auto`

*最小の長さ*: 1

### `.actions[].id`

**アクションID**

アクションを識別する一意な文字列を設定します。(必須)

*型*: string

*最小の長さ*: 1

### `.actions[].attach`

**添付**

アクションにステートカードを添付することを許可する場合はtrueを指定します。省略値:false

*型*: boolean

### `.actions[].roles`

**所有ロール**

このアクションを使用可能なロールのIDの配列を設定します。allを設定するとすべてのユーザが使用可能となります。(必須)

*型*: array

### `.actions[].roles[]`

*型*: string

### `.actions[].description`

**説明**

アクションの説明を記述します。

*型*: string

### `.actions[].attachments`

**ステートカード添付**

アクションに添付するステートカードのIDを指定します。省略値:なし

*型*: array

### `.actions[].attachments[]`

*型*: number

### `.actions[].phase`

**フェーズ**

このアクションが利用可能なフェーズを設定します。フェーズ番号の配列を指定します。省略値：すべてのフェーズで使用可能

*型*: array

### `.actions[].phase[]`

*型*: number

### `.actions[].to`

**宛先**

自動アクションの宛先ロールを指定します。type:autoの場合は必須です。

*型*: string

### `.actions[].from`

**送信元**

自動アクションの送信元ロールを指定します。type:autoの場合は必須です。

*型*: string

### `.actions[].addstate`

**追加システムステート**

アクションの受付時に追加するシステムステートを設定します。

*型*: array

### `.actions[].addstate[]`

*型*: string

### `.actions[].removestate`

**削除システムステート**

アクションの受付時に削除するシステムステートを設定します。

*型*: array

### `.actions[].removestate[]`

*型*: string

### `.actions[].comment`

**コメント**

アクションに添付するメッセージのデフォルト値を設定します。省略値:なし

*型*: string

### `.actions[].statecondition`

**ステート条件**

アクションを使用するために必要なステートカードのIDのセットを指定します。ユーザは、このプロパティに指定されたすべてのステートカードを入手しないと、「新規アクション」画面からこのアクションを選択することができません。省略値:なし(常に使用可能)

*型*: array

### `.actions[].statecondition[]`

*型*: string

### `.actions[].timecondition`

**経過時間条件**

前提条件となるアクションを実行後に一定時間経過後にアクションの実行を許可するために使用します。省略値:なし(常に使用可能)

*型*: array

### `.actions[].timecondition[]`

### `.actions[].timecondition[]prerequisite`

**前提アクション**

アクションを実行するための前提条件となるアクションのIDを指定します。

*型*: string

### `.actions[].timecondition[]elapsed`

**経過時間**

条件となるアクション実行後の経過時間(秒)を指定します。

*型*: number

### `.actions[].delay`

**自動アクション遅延時間**

自動アクションが実行可能になってから実行するまでの遅延時間(秒)を指定します。

*型*: number

### `.actions[].systemstatecondition`

**システムステート条件**

自動アクション(type:auto)の実行の契機となるシステムステートのIDと論理演算子を指定します。省略値:なし(無条件に実行する)

*型*: array

### `.actions[].systemstatecondition[]`

*型*: string