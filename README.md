## 安卓版本升级中存在的一些问题记录，包含部分AndroidQ的适配


### 明文HTTP限制
当targetSdkVersion >= Build.VERSION_CODES.P 时，默认限制了HTTP请求

1.在AndroidManifest.xml中Application添加如下节点代码<application android:usesCleartextTraffic="true">

2.在AndroidManifest.xml中Application添加如下节点代码<application android:networkSecurityConfig="@xml/network_security_config">

### Only fullscreen activities can request orientation 异常
1.该问题出现在 targetSdkVersion >= Build.VERSION_CODES.O_MR1 ，也就是 API 27,当设备为Android 26时（27以上已经修复，也许google觉得不妥当，又改回来了），
如果非全面屏透明activity固定了方向，则出现该异常，但是当我们在小米、魅族等Android 26机型测试的时候，并没有该异常，华为机型则报该异常，
这是何等的卧槽。。。没办法，去掉透明style或者去掉固定方向代码即可，其它无解

### 安装APK Intent及其它文件相关Intent

```groovy
	/*
	* 自Android N开始，是通过FileProvider共享相关文件，但是Android Q对公有目录 File API进行了限制
	* 从代码上看，又变得和以前低版本一样了，只是必须加上权限代码Intent.FLAG_GRANT_READ_URI_PERMISSION
	*/ 
	fun installApk(context: Context,apkFilePath: String,providerPath: String ="${context.packageName}.FileProvider"){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //适配Android Q,注意apkFilePath是通过ContentResolver得到的
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(apkFilePath), "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }else{
            val apkFile = File(apkFilePath)
            if (!apkFile.exists()) return
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val contentUri = FileProvider.getUriForFile(context, providerPath, apkFile)
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        }
    }
```	

### AndroidQ中对于文件路径做了修改

1.使用官方支持的规避方案：通过设置android:requestLegacyExternalStorage="false"属性来关闭APP的沙盒机制。

2.沙盒模式下，每个APP在访问sdcard时会进入过滤视图，只能访问私有路径（Context.getExternalFilesDir()）和公共存储空间（多媒体，MediaStore）,
除了第一种写在“/sdcard/Android/data/packageName/file”路径中可以正常使用InputStream&OutputStream读写文件，另外两种方法都无法直接对文件IO操作。
所以后续有向SDcard中写文件的需求优先向APP私有路径中写入
MediaStore是外部存储空间的公共媒体集合，存放的都是多媒体文件，在API >= 29后加入了download集合

Android Q 公有目录只能通过Content Uri + id的方式访问，以前的File路径全部无效

照片：存储在 MediaStore.Images 中

视频：存储在 MediaStore.Video 中

音乐文件：存储在 MediaStore.Audio 中

下载文件：存储在 MediaStore.Downloads 中

所有文件：存储在 MediaStore.Files 中






