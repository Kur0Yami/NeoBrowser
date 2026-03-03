package com.neobrowser.browser
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object DownloadHelper {
    fun handle(ctx:Context,url:String,cd:String,mime:String,manager:String,customPkg:String) {
        val fn = cd.substringAfter("filename=").trim().ifEmpty { Uri.parse(url).lastPathSegment?:"download" }
        when(manager) {
            "ask"    -> showDialog(ctx,url,cd,mime,fn,customPkg)
            "1dm+"   -> launch(ctx,"idm.internet.download.manager.plus",url)
            "idm+"   -> launch(ctx,"idm.internet.download.manager.adm",url)
            "adm"    -> launch(ctx,"com.dv.adm",url)
            "custom" -> if(customPkg.isNotEmpty()) launch(ctx,customPkg,url) else sysDown(ctx,url,mime,fn)
            else     -> sysDown(ctx,url,mime,fn)
        }
    }
    private fun showDialog(ctx:Context,url:String,cd:String,mime:String,fn:String,cp:String) {
        val opts = mutableListOf("System")
        listOf("idm.internet.download.manager.plus" to "1DM+",
               "idm.internet.download.manager.adm"  to "IDM+",
               "com.dv.adm" to "ADM").forEach { (p,l) -> if(isInst(ctx,p)) opts.add(l) }
        AlertDialog.Builder(ctx).setTitle("Download: \$fn")
            .setItems(opts.toTypedArray()) { _,i ->
                when(opts[i]) {
                    "1DM+" -> launch(ctx,"idm.internet.download.manager.plus",url)
                    "IDM+" -> launch(ctx,"idm.internet.download.manager.adm",url)
                    "ADM"  -> launch(ctx,"com.dv.adm",url)
                    else   -> sysDown(ctx,url,mime,fn)
                }
            }.show()
    }
    private fun launch(ctx:Context,pkg:String,url:String) {
        if(!isInst(ctx,pkg)){ Toast.makeText(ctx,"\$pkg not installed",Toast.LENGTH_SHORT).show();return }
        ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).setPackage(pkg))
    }
    private fun sysDown(ctx:Context,url:String,mime:String,fn:String) {
        val req=DownloadManager.Request(Uri.parse(url)).setTitle(fn).setMimeType(mime)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fn)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        Toast.makeText(ctx,"Downloading...",Toast.LENGTH_SHORT).show()
    }
    private fun isInst(ctx:Context,pkg:String)=try{ctx.packageManager.getPackageInfo(pkg,0);true}catch(e:Exception){false}
}
