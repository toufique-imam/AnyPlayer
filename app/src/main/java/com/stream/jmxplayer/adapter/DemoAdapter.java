//package com.stream.jmxplayer.adapter;
//
//import android.app.Activity;
//import android.app.DownloadManager;
//import android.content.Context;
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Environment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.RatingBar;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.stream.jmxplayer.R;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.io.File;
//import java.util.ArrayList;
//
//class SampleData {
//    String link, title, description, imageLink, time;
//    Float rating;
//}
//
//class DemoViewHolder extends RecyclerView.ViewHolder {
//    TextView title, description, timer;
//    RatingBar ratingBar;
//    ImageView imageView;
//    ImageButton downloadButton;
//
//    public DemoViewHolder(@NonNull @NotNull View itemView) {
//        super(itemView);
//        title = itemView.findViewById(R.id.titleText);
//        description = itemView.findViewById(R.id.descriptionText);
//        timer = itemView.findViewById(R.id.timeText);
//        ratingBar = itemView.findViewById(R.id.simpleRatingBar);
//        imageView = itemView.findViewById(R.id.imageView);
//        downloadButton = itemView.findViewById(R.id.downloadButton);
//    }
//}
//
//public class DemoAdapter extends RecyclerView.Adapter<DemoViewHolder> {
//    ArrayList<SampleData> data;
//    Activity activity;
//    Downloader downloader;
//
//    DemoAdapter(Activity activity, ArrayList<SampleData> data) {
//        this.data = data;
//        this.activity = activity;
//        downloader = new Downloader();
//    }
//
//    public void addData(ArrayList<SampleData> data) {
//        int prevSize = data.size();
//        this.data.addAll(data);
//        notifyItemRangeInserted(prevSize, data.size());
//    }
//
//    public void updateData(ArrayList<SampleData> data) {
//        this.data = data;
//        notifyDataSetChanged();
//    }
//
//    public void removeData(int i) {
//        this.data.remove(i);
//        notifyItemRemoved(i);
//    }
//
//    public void removeRangeData(int i, int cnt) {
//        for (int x = 0; x < cnt; x++) {
//            this.data.remove(i + x);
//        }
//        notifyItemRangeRemoved(i, cnt);
//    }
//
//
//    @NonNull
//    @NotNull
//    @Override
//    public DemoViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.remove_later, parent);
//        return new DemoViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull @NotNull DemoViewHolder holder, int position) {
//        SampleData sampleData = data.get(position);
//        Glide.with(holder.imageView).load(sampleData.imageLink).into(holder.imageView);
//        holder.description.setText(sampleData.description);
//        holder.title.setText(sampleData.title);
//        holder.timer.setText(sampleData.time);
//        holder.ratingBar.setRating(sampleData.rating);
//        holder.downloadButton.setOnClickListener(v -> downloader.startDownload(sampleData.title, sampleData.link, new ProgressListener() {
//            @Override
//            public void onProgressUpdate(int percent) {
//
//            }
//
//            @Override
//            public void onDownloadDone() {
//                holder.downloadButton.setVisibility(View.GONE);
//            }
//
//            @Override
//            public void onDownloadStarted() {
//                holder.downloadButton.setImageResource(R.drawable.ic_download);
//            }
//        }));
//    }
//
//    @Override
//    public int getItemCount() {
//        return data.size();
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return (data.get(position).title + data.get(position).link).hashCode();
//    }
//
//    interface ProgressListener {
//        void onProgressUpdate(int percent);
//
//        void onDownloadDone();
//
//        void onDownloadStarted();
//    }
//
//
//    class Downloader {
//        void startDownload(String title, String link, ProgressListener progressListener) {
//            DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
//
//            String PATH = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
//
//            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
//
//            request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, title);
//            File outputFile = new File(PATH, title);
//            if (outputFile.exists()) {
//                outputFile.delete();
//            }
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
//            long DownloadManagerId = manager.enqueue(request);
//            progressListener.onDownloadStarted();
//            new Thread(() -> {
//                while (true) {
//                    try {
//                        DownloadManager.Query q = new DownloadManager.Query();
//                        q.setFilterById(DownloadManagerId); //filter by id which you have receieved when reqesting download from download manager
//                        Cursor cursor = manager.query(q);
//                        //GlobalFunctions.logger("Download Manager", DownloadManagerId + " " + manager);
//                        cursor.moveToFirst();
//
//                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
//                            break;
//                        } else if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_FAILED) {
//                            break;
//                        }
//                        int bytes_downloaded = cursor.getInt(cursor
//                                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
//                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
//                        final int dl_progress = (int) ((bytes_downloaded * 100L) / bytes_total);
//
//                        activity.runOnUiThread(() -> {
//                            progressListener.onProgressUpdate(dl_progress);
//                        });
//                        cursor.close();
//                    } catch (Exception e) {
//                        progressListener.onDownloadDone();
//                    }
//                }
//                progressListener.onDownloadDone();
//            }).start();
//
//        }
//    }
//}
//    <?xml version="1.0" encoding="utf-8"?>
//<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
//        android:layout_width="match_parent"
//        android:layout_height="wrap_content"
//        android:orientation="horizontal"
//        android:padding="5dp">
//
//<ImageView
//        android:id="@+id/imageView"
//                android:layout_width="64dp"
//                android:layout_height="64dp"
//                android:layout_gravity="center_vertical"
//                android:src="@drawable/ic_download" />
//
//<LinearLayout
//        android:layout_width="0dp"
//                android:layout_height="wrap_content"
//                android:layout_weight="1"
//                android:orientation="vertical"
//                android:padding="5dp">
//
//<LinearLayout
//            android:layout_width="match_parent"
//                    android:layout_height="wrap_content">
//
//<TextView
//                android:id="@+id/titleText"
//                        android:layout_width="wrap_content"
//                        android:layout_height="wrap_content"
//                        android:layout_marginEnd="10dp"
//                        android:text="title"
//                        android:textSize="15sp"
//                        android:textStyle="bold" />
//
//<TextView
//                android:id="@+id/timeText"
//                        android:layout_width="wrap_content"
//                        android:layout_height="wrap_content"
//                        android:text="time"
//                        android:textSize="13sp"
//                        android:textStyle="bold" />
//
//
//</LinearLayout>
//
//<TextView
//            android:id="@+id/descriptionText"
//                    android:layout_width="match_parent"
//                    android:layout_height="wrap_content"
//                    android:lines="2"
//                    android:text="description" />
//
//<RatingBar
//            android:id="@+id/simpleRatingBar"
//                    android:layout_width="230dp"
//                    android:layout_height="wrap_content"
//                    android:isIndicator="true"
//                    android:max="5"
//                    android:numStars="5"
//                    android:rating="3.5"
//                    android:scaleX=".5"
//                    android:scaleY=".5"
//                    android:transformPivotX="0dp"
//                    android:transformPivotY="0dp" />
//</LinearLayout>
//
//<ImageButton
//        android:id="@+id/downloadButton"
//                android:layout_width="64dp"
//                android:layout_height="64dp"
//                android:layout_gravity="center_vertical"
//                android:src="@drawable/ic_download" />
//
//</LinearLayout>