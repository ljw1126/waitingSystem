## 테스트 준비
### Apache JMeter 설치
- [다운로드 바로가기](https://jmeter.apache.org/download_jmeter.cgi)
- `*.tgz` 파일 다운로드

**압축 해제**
```shell
$ tar -xvf apahce-jmeter-5.6.3.tgz
```

**실행하기**
```shell
$ ./apache-jmeter-5.6.3/bin/jmeter
```
<br/>

### Plugin Manager 설치
- [다운로드 바로가기](https://jmeter-plugins.org/wiki/PluginsManager/)
- `*.jar` 다운로드 후 `{apache-jemter-*}/lib/ext` 디렉터리에 넣고 JMeter 재시작

**파일 이동**
```shell
$ mv jmeter-plugins-manager-1.10.jar {jmeter 디렉터리}/lib/ext
```
<br/>

### IP 확인 및 방화벽 허용
- 잡음을 고려해 테스트와 서버를 분리 
  - ex. JMeter(M1 Mac Pro) ➡️ Desktop(Ubuntu)

**IP 확인**
```shell
$ hostname -I | awk '{print $1}'
```

**방화벽 허용**
```shell
$ sudo ufw allow 9090/tcp
$ sudo ufw reload
```
호출하는 쪽에서 ping이나 브라우저 접속을 통해 확인
