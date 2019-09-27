# points.json:ポイントカードのセット

演習のスコアを採点するためのポイントカードのセットを定義します。

### Example

```json
{
  "pointcard": [
    {
      "phase": -1,
      "before": 0,
      "after": 0,
      "statecondition": [
        {}
      ],
      "multiplicity": 1,
      "point": 0
    }
  ]
}
```

### Documentation

### `.version`

**バージョン情報を記述します。**

### `.pointcard`

**ポイントカードを定義します。ポイントカードはシナリオの進行に応じて加点・減点される規則を表現します。
システムステートが更新されるごとにポイントカードが評価され、条件を満たしたポイントカードが演習チームに発行されます。
ポイントカードの発行条件はstateおよびstateconditionプロパティとによって設定します。**

*型*: array

*最小項目*: 1

### `.pointcard[]`

### `.pointcard[].phase`

**対象とする演習フェーズを指定します。規定値:-1(すべてのフェーズを対象)**

*型*: number

### `.pointcard[].description`

**説明文を指定します。**

*型*: string

*最小の長さ*: 1

### `.pointcard[].before`

**ポイント付与の条件となるフェーズ開始からの経過時間の最小値(秒数)を指定します。規定値:制限なし**

*型*: number

### `.pointcard[].after`

**ポイント付与の条件となるフェーズ開始からの経過時間の最大値(秒数)を指定します。規定値:制限なし**

*型*: number

### `.pointcard[].state`

**ポイント付与の対象とするシステムステートのIDを指定します。規定値:-1(条件なし)**

*型*: number

### `.pointcard[].statecondition`

**ポイント付与の前提条件となるシステムステートのIDと論理演算子を指定します。規定値:なし**

*型*: array

### `.pointcard[].statecondition[]`

### `.pointcard[].multiplicity`

**同一のポイントカードの発行数の上限を指定します。2以上の数を指定すると、条件を満たすたびに同一のポイントカードが複数発行されます。規定値:1**

*型*: number

*最小値*: 1

### `.pointcard[].point`

**ポイント数を指定します。このポイントカードによってチームスコアに加点される整数です。規定値:0**

*型*: number

### `.pointcard[].name`

**ポイントカードの名前を指定します。**

*型*: string

### `.pointcard[].id`

**ポイントカードを識別するIDを指定します。(必須)**

*型*: string

*最小の長さ*: 1