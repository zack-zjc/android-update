## 安卓版本升级中存在的一些问题记录，包含部分AndroidQ的适配,持续更新备份

### 1.明文HTTP限制
当targetSdkVersion >= Build.VERSION_CODES.P 时，默认限制了HTTP请求

1.在AndroidManifest.xml中Application添加如下节点代码

android:usesCleartextTraffic="true"

2.在AndroidManifest.xml中Application添加如下节点代码

android:networkSecurityConfig="@xml/network_security_config"

### 2.Only fullscreen activities can request orientation 异常
1.该问题出现在 targetSdkVersion >= Build.VERSION_CODES.O_MR1 ，也就是 API 27,当设备为Android 26时（27以上已经修复，也许google觉得不妥当，又改回来了），
如果非全面屏透明activity固定了方向，则出现该异常，但是当我们在小米、魅族等Android 26机型测试的时候，并没有该异常，华为机型则报该异常，
这是何等的卧槽。。。没办法，去掉透明style或者去掉固定方向代码即可，其它无解

### 3.Apache HTTP 客户端弃用影响采用非标准 ClassLoader 的应用

1.Target < 28 可用在build.gradle中添加继续引用

```groovy
  android {
    useLibrary 'org.apache.http.legacy'
  }
```	

2.Target >= 28 默认情况下该内容库已从 bootclasspath 中移除且不可用于应用,所以在AndroidManifest.xml中添加

```groovy
  <uses-library android:name="org.apache.http.legacy" android:required="false"/>
```	

### 4.全面屏适配

```groovy

  <application android:resizeableActivity="true"/>
  
  <meta-data
        android:name="android.max_aspect"
        android:value="1317014784.000000"/>
        
  <meta-data
        android:name="android.min_aspect"
        android:value="1065353216.000000"/>

```	

### 5.文件路径适配
在AndroidManifest.xml中添加适配文件选择

```groovy
  <provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.FileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
      android:name="android.support.FILE_PROVIDER_PATHS"
      android:resource="@xml/file_paths" />
  </provider>          

```	

        

### 6.安装APK Intent及其它文件相关Intent

```groovy
  //安装apk需要的权限
  <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
```	

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

### 7.AndroidQ中对于文件路径做了修改

1.使用官方支持的规避方案：通过设置android:requestLegacyExternalStorage="false"属性来关闭APP的沙盒机制。

2.沙盒模式下，每个APP在访问sdcard时会进入过滤视图，只能访问私有路径（Context.getExternalFilesDir()）和公共存储空间（多媒体，MediaStore）,
除了第一种写在“/sdcard/Android/data/packageName/file”路径中可以正常使用InputStream&OutputStream读写文件，另外两种方法都无法直接对文件IO操作,
Android Q 公有目录只能通过Content Uri + id的方式访问，以前的File路径全部无效,所以后续有向SDcard中写文件的需求优先向APP私有路径中写入
MediaStore是外部存储空间的公共媒体集合，存放的都是多媒体文件，在API >= 29后加入了download集合

照片：存储在 MediaStore.Images 

视频：存储在 MediaStore.Video 

音乐文件：存储在 MediaStore.Audio 

下载文件：存储在 MediaStore.Downloads 

所有文件：存储在 MediaStore.Files 

### 8.资源文件适配
屏幕比例从16:9变成18:9，对于全屏铺满显示的图片，往往被会拉伸导致变形，针对这种问题，我们以分辨率为2160X1080，像素密度为480dpi的VIVO X20Plus手机为例，
可以在资源目录下面增加一个文件夹，drawable-h642dp-port-xxhdpi，并将GUI切好的分辨率为2160X1080资源图片放在这个目录下，系统就能自动使用这种图片，便不会出现拉伸的问题。
关于h<N>dp的详细用法，google开发者文档也有详细介绍：https://developer.android.com/guide/practices/screens_support.html

### 9.gradle修改输出应用名称
```groovy
applicationVariants.all { variant ->    
      variant.outputs.all {         
          if (outputFileName.endsWith('.apk')){
                def fileName = "应用名.apk"
                if (outputFileName.endsWith('.apk')){
                    if ("debug".equalsIgnoreCase(variant.buildType.name)){
                          fileName = "应用名-debug-${variant.versionName}.apk"
                    }else{
                          fileName = "应用名-${variant.versionName}.apk"
                    }
                }
                outputFileName = fileName
            }
      }
 }
```	
### 10.安卓通知
参考https://www.jianshu.com/p/cb8426620e74

### 11.安卓10:Scoped Storage 
1.多媒体文件需要使用 MediaStore API 访问

2.其他文件需要使用 Storage Access Framework API 访问

















