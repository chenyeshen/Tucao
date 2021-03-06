package me.sweetll.tucao.business.video.viewmodel

import android.app.ActivityOptions
import android.databinding.BindingAdapter
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import me.sweetll.tucao.R
import me.sweetll.tucao.base.BaseViewModel
import me.sweetll.tucao.business.download.model.Part
import me.sweetll.tucao.business.uploader.UploaderActivity
import me.sweetll.tucao.business.video.adapter.DownloadPartAdapter
import me.sweetll.tucao.business.video.fragment.VideoInfoFragment
import me.sweetll.tucao.extension.DownloadHelpers
import me.sweetll.tucao.extension.HistoryHelpers
import me.sweetll.tucao.extension.load
import me.sweetll.tucao.extension.sanitizeHtml
import me.sweetll.tucao.model.json.Result
import me.sweetll.tucao.widget.CustomBottomSheetDialog
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*

class VideoInfoViewModel(val videoInfoFragment: VideoInfoFragment): BaseViewModel() {
    val result: ObservableField<Result> = ObservableField()
    val isStar = ObservableBoolean()
    val create = ObservableField<String>()
    val avatar = ObservableField<String>()

    var signature = ""
    var headerBg = ""

    companion object {
        @BindingAdapter("app:imageUrl")
        @JvmStatic
        fun loadImage(imageView: ImageView, url: String?) {
            url?.let {
                imageView.load(imageView.context, it, R.drawable.default_avatar)
            }
        }
    }

    fun bindResult(result: Result) {
        this.result.set(result)
        this.isStar.set(checkStar(result))

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        this.create.set("发布于${sdf.format(Date(result.create.toLong() * 1000))}")

        // 获取头像
        rawApiService.user(result.userid)
                .bindToLifecycle(videoInfoFragment)
                .sanitizeHtml {
                    parseAvatar(this)
                }
                .subscribe({
                    avatar.set(it)
                }, {
                    error ->
                    error.printStackTrace()
                })
    }

    fun checkStar(result: Result): Boolean = HistoryHelpers.loadStar()
            .any { it.hid == result.hid }

    fun onClickDownload(view: View) {
        if (result.get() == null) return
        val dialog = CustomBottomSheetDialog(videoInfoFragment.activity)
        val dialogView = LayoutInflater.from(videoInfoFragment.activity).inflate(R.layout.dialog_pick_download_video, null)
        dialog.setContentView(dialogView)

        dialogView.findViewById(R.id.img_close).setOnClickListener {
            dialog.dismiss()
        }

        val partRecycler = dialogView.findViewById(R.id.recycler_part) as RecyclerView
        val partAdapter = DownloadPartAdapter(
                videoInfoFragment.parts
                        .map {
                            it.copy().apply { checked = false }
                        }
                        .toMutableList()
        )

        val startDownloadButton = dialog.findViewById(R.id.btn_start_download) as Button
        startDownloadButton.setOnClickListener {
            view ->
            val checkedParts = partAdapter.data.filter({
                p ->
                !p.checkDownload() && p.checked
            })
            DownloadHelpers.startDownload(videoInfoFragment.activity, result.get().copy().apply {
                video = video.filter {
                    v ->
                    checkedParts.any { v.vid == it.vid }
                }.toMutableList()
            })
            dialog.dismiss()
        }

        partRecycler.addOnItemTouchListener(object: OnItemClickListener() {
            override fun onSimpleItemClick(helper: BaseQuickAdapter<*, *>, view: View, position: Int) {
                val part = helper.getItem(position) as Part
                part.checked = !part.checked
                helper.notifyItemChanged(position)
                startDownloadButton.isEnabled = partAdapter.data.any({
                    p ->
                    !p.checkDownload() && p.checked
                })
            }

        })

        (partRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        partRecycler.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        partRecycler.adapter = partAdapter

        val pickAllButton = dialog.findViewById(R.id.btn_pick_all) as Button
        pickAllButton.setOnClickListener {
            view ->
            if (partAdapter.data.all { it.checked }) {
                // 取消全选
                startDownloadButton.isEnabled = false
                pickAllButton.text = "全部选择"
                partAdapter.data.forEach {
                    item ->
                    item.checked = false
                }
            } else {
                // 全选
                startDownloadButton.isEnabled = true
                pickAllButton.text = "取消全选"
                partAdapter.data.forEach {
                    item ->
                    item.checked = true
                }
            }
            partAdapter.notifyDataSetChanged()
        }

        dialog.show()
    }

    fun onClickStar(view: View) {
        if (result.get() == null) return
        if (isStar.get()) {
            HistoryHelpers.removeStar(result.get())
            isStar.set(false)
        } else {
            HistoryHelpers.saveStar(result.get())
            isStar.set(true)
        }
    }

    fun onClickUser(view: View) {
        if (headerBg.isNotEmpty()) {
            val options: Bundle? = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    videoInfoFragment.activity, view.findViewById(R.id.avatarImg), "transition_avatar"
            ).toBundle()
            UploaderActivity.intentTo(videoInfoFragment.activity, result.get().userid, result.get().user, avatar.get(), signature, headerBg, options)
        }
    }

    private fun parseAvatar(doc: Document): String {
        val header_div = doc.select("div.header")[0]
        val style = header_div.child(0).attr("style")
        headerBg = style.substring(style.indexOf("http://"), style.indexOf(")"))

        val userinfo_div = doc.select("div.userinfo").getOrNull(0)
        userinfo_div?.let {
            val lis = userinfo_div.child(0)
            val signature_li = lis.children().last()
            signature = signature_li.text().substring(5)
        }

        val avatar_div = doc.select("div.avatar")[0]
        return avatar_div.child(0).child(0).attr("src")
    }

}
