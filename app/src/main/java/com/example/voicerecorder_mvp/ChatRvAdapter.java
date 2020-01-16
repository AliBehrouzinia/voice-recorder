package com.example.voicerecorder_mvp;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voicerecorder_mvp.pojo.VoiceMessage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import rm.com.audiowave.AudioWaveView;
import rm.com.audiowave.OnProgressListener;


public class ChatRvAdapter extends RecyclerView.Adapter<ChatRvAdapter.ViewHolder> {
    @BindDrawable(R.drawable.ic_play)
    Drawable playIcon;
    @BindDrawable(R.drawable.ic_pause)
    Drawable pauseIcon;

    private final MainPresenter presenter;
    private List<VoiceMessage> voiceMessages;
    private Context context;
    private ItemClickListener mClickListener;
    private boolean isPlaying;
    private int last_index = -1;
    private ViewHolder viewHolder;


    public ViewHolder getViewHolder() {
        return viewHolder;
    }

    public void setViewHolder(ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    ChatRvAdapter(Context context, List<VoiceMessage> data, MainPresenter presenter) {
        this.context = context;
        this.voiceMessages = data;
        this.presenter = presenter;
    }

    @NonNull
    @Override
    public ChatRvAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.voice_item, parent, false);
        this.viewHolder = new ViewHolder(view);
        return viewHolder;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(@NonNull ChatRvAdapter.ViewHolder holder, int position) {
        VoiceMessage voiceMessage = voiceMessages.get(position);

        File file = new File(voiceMessage.getPath());


        holder.seekBar.setRawData(presenter.getVoiceRawData(position));
        holder.seekBar.setProgress(convertCurrentMediaPositionIntoPercent(voiceMessage.getLastProgress(), voiceMessage.getDuration()));
        holder.dateModified.setText(voiceMessage.getDateModified());

        if (voiceMessage.isPlaying()) {
            holder.playButton.setImageResource(R.drawable.ic_pause);
            holder.updateSeekBar(holder);
        } else {
            if (presenter.isPlaying()) {
                voiceMessage.setLastProgress(0);
                holder.seekBar.setProgress(voiceMessage.getLastProgress());
            }
            holder.playButton.setImageResource(R.drawable.ic_play);
            holder.timer.setText(calculateTime(voiceMessage.getDuration()));
        }

        bindEvents(holder, voiceMessage, position);
    }

    private void bindEvents(ViewHolder holder, VoiceMessage voiceMessage, int position) {
        holder.seekBar.setOnProgressListener(new OnProgressListener() {
            @Override
            public void onStartTracking(float v) {

            }

            @Override
            public void onStopTracking(float v) {

            }

            @Override
            public void onProgressChanged(float progress, boolean fromUser) {
                int position = (int)((progress / 100) * voiceMessage.getDuration());
                holder.timer.setText(calculateTime(position));
                if (presenter.getMediaPlayer() != null && presenter.getVoiceMessage().getPath().equals(voiceMessage.getPath()) && fromUser) {
                    presenter.isUserSeeking = true;
                    presenter.seek(position);
                    voiceMessage.setLastProgress(position);
                } else if (position == presenter.getPosition() || fromUser) {
                    voiceMessage.setLastProgress(position);
                }
            }
        });

    }

    private String calculateTime(Integer duration) {
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds - (minutes * 60);
        return minutes + ":" + presenter.checkSecondsDigit(seconds);

    }

    private float convertCurrentMediaPositionIntoPercent(int currentPosition, int duration) {
        if (duration < 1){
            return 0;
        }
        return currentPosition * 100 / duration;
    }


    @Override
    public int getItemCount() {
        return voiceMessages.size();
    }

    private void markAllPaused() {
        for (int i = 0; i < voiceMessages.size(); i++) {
            voiceMessages.get(i).setPlaying(false);
            voiceMessages.set(i, voiceMessages.get(i));
        }
        notifyDataSetChanged();
    }

    VoiceMessage getItem(int id) {
        return voiceMessages.get(id);
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, MainContract.Adapter {
        @BindView(R.id.play_button)
        ImageView playButton;
        @BindView(R.id.seek_bar)
        AudioWaveView seekBar;
        @BindView(R.id.date)
        TextView dateModified;
        @BindView(R.id.timer)
        TextView timer;
        @BindView(R.id.delete)
        ImageView deleteIcon;

        private int position;
        private ViewHolder holder;
        private Handler mHandler = new Handler();


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
            bindEvents();
        }

        private void bindEvents() {
            deleteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int deletingVoicePosition = getAdapterPosition();
                    presenter.deleteVoice(deletingVoicePosition);
                }});

            playButton.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View view) {
                    position = getAdapterPosition();
                    VoiceMessage voiceMessage = voiceMessages.get(position);
                    if (!presenter.isRecording()) {
                    if (voiceMessage.isPlaying()) {
                        if (position == last_index) {
                            voiceMessage.setPlaying(false);
                            presenter.setPlaying(false);
                            voiceMessages.set(position, voiceMessage);
                            presenter.stopPlay();
                            notifyDataSetChanged();
                        } else {
                            markAllPaused();
                            presenter.stopPlay();
                            presenter.startPlay(position);
                            presenter.setPlaying(true);
                            voiceMessage.setPlaying(true);
                            notifyDataSetChanged();
                            last_index = position;
                        }

                    } else {
                        if (position == last_index) {
                            markAllPaused();
                            presenter.startPlay(position);
                            voiceMessage.setPlaying(true);
                            presenter.setPlaying(true);
                            notifyDataSetChanged();

                            last_index = position;
                        } else {
                            markAllPaused();
                            presenter.stopPlay();
                            presenter.startPlay(position);
                            voiceMessage.setPlaying(true);
                            presenter.setPlaying(true);
                            notifyDataSetChanged();
                            last_index = position;
                        }

                    }
                    }
                }
            });
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                viewHolder.updateSeekBar(holder);
            }
        };

        private void updateSeekBar(ViewHolder holder) {
            this.holder = holder;
            if (presenter.getMediaPlayer() != null) {
                int currentPosition = presenter.getMediaPlayer().getCurrentPosition();
                //this.holder.seekBar.setMax(presenter.getMediaPlayer().getDuration());
                this.holder.seekBar.setProgress(convertCurrentMediaPositionIntoPercent(currentPosition, presenter.getVoiceMessage().getDuration()));
                presenter.getVoiceMessage().setLastProgress(currentPosition);
            }
            mHandler.postDelayed(runnable, MainPresenter.SEEK_BAR_UPDATE_DELAY);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public void startPlaying() {
            //seekBar.setProgress(presenter.getLastProgress());
            presenter.seek(presenter.getLastProgress());
        }

        @Override
        public void stopPlaying() {
            mHandler.removeCallbacks(runnable);
        }

    }
}
