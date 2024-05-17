# Jar - JVM settings
##### -Xmx65535m
##### -Dsun.java2d.opengl=true -Dsun.java2d.ddscale=true -Dawt.nativeDoubleBuffering=true  -Dsun.java2d.ddoffscreen=false -Dsun.java2d.d3dtexbpp=16


# Build & Run
## Build
```shell
mvn clean package
```

### 每秒允许120次更新推送，不要求同步所有预览(断续更新)
120 updates per second, no need to sync all previews (intermittent updates)
```shell
java -jar .\WBServer.jar 127.0.0.1 8611 WB1 -RCMD 180 -SHOWALL false -Dsun.java2d.opengl=true -Dsun.java2d.ddscale=true -Dawt.nativeDoubleBuffering=true  -Dsun.java2d.ddoffscreen=false -Dsun.java2d.d3dtexbpp=16
```
```shell
java -jar .\WBClient.jar 127.0.0.1 8611 WinstonLi
```

### 未定义白板名 每秒允许240次更新推送，要求同步所有预览（会导致卡顿）
Underfined whiteboard name, 240 updates per second, sync all previews (may cause lag)
```shell
java -jar .\WBServer.jar 127.0.0.1 8611 -RCMD 240 -SHOWALL true -DDL 5
```
```shell
java -jar .\WBClient.jar 127.0.0.1 8611 WinstonLi
```

### 降低延迟容忍 DDL设为较小值 Lower the delay tolerance, set the DDL to a small value
- 注意，客户侧容忍度为DDL+2 Note that the client side tolerance is DDL+2
```shell
java -jar  .\WBServer.jar 127.0.0.1 8611 -RCMD 240 -SHOWALL true -DDL 2 
```
```shell
java -jar .\WBClient.jar 127.0.0.1 8611 TonyMa
```


### 关闭流控
Turn off flow control explicitly (Similar to the ample default setting)
```shell
java -jar .\WBServer.jar 127.0.0.1 8611 WB1 -SHOWALL true -FCOFF true
```
```shell
java -jar .\WBClient.jar 127.0.0.1 8611 StephenJobs
```



## Server & client start example：
## Command Line Options

| Option  | Description | Usage | Example |
|---------|-------------|-------|---------|
| `-RCMD` | 控制每秒允许的更新次数，影响数据推送的频率。<br><span style="color: gray; font-style: italic;">Controls the number of updates allowed per second, affecting the data push frequency.</span> | `-RCMD <number>` | `-RCMD 120` 表示每秒更新120次。<br><span style="color: gray; font-style: italic;">`-RCMD 120` means 120 updates per second.</span> |
| `-SHOWALL` | 设置是否同步所有预览。启用此选项可能增加网络负载和系统延迟。<br><span style="color: gray; font-style: italic;">Sets whether to sync all previews. Enabling this option may increase network load and system latency.</span> | `-SHOWALL <true\false>` | `-SHOWALL true` 同步所有预览。<br><span style="color: gray; font-style: italic;">`-SHOWALL true` to sync all previews.</span> `SHOWALL false` 不完全同步，提高性能。<br><span style="color: gray; font-style: italic;">`SHOWALL false` for unsynced previews, improves performance.</span> |
| `-DDL` | 设置延迟容忍度，单位为秒。绘制操作若超过此时间则被取消，控制操作的实时性。<br><span style="color: gray; font-style: italic;">Sets the delay tolerance in seconds. Operations exceeding this time limit will be canceled, controlling the timeliness of operations.</span> | `-DDL <seconds>` | `-DDL 3` 如果绘制延迟超过3秒，则取消。<br><span style="color: gray; font-style: italic;">`-DDL 3` cancels the operation if the delay exceeds 3 seconds.</span> |
| `-FCOFF` | 控制是否关闭流量控制。关闭流控可以减少延迟，但可能增加网络拥堵风险。<br><span style="color: gray; font-style: italic;">Controls whether to turn off flow control. Turning off flow control can reduce latency but may increase the risk of network congestion.</span> | `-FCOFF <true\false>` | `-FCOFF true` 关闭流控功能。<br><span style="color: gray; font-style: italic;">`-FCOFF true` to turn off flow control.</span> |
