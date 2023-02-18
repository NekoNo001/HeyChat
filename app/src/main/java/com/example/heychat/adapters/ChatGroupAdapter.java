package com.example.heychat.adapters;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.databinding.ItemContainerReceivedMessageGroupBinding;
import com.example.heychat.databinding.ItemContainerSentMessageBinding;
import com.example.heychat.listeners.MessageListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.FontSize;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;
import java.util.Objects;


public class ChatGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private Bitmap receiverProfileImage;
    private final String senderId;
    public MessageListener messageListener;
    private PreferenceManager preferenceManager;

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
    private static final int VIEW_TYPE_SENT_FILE = 5;
    private static final int VIEW_TYPE_RECEIVED_FILE = 6;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    public ChatGroupAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId, MessageListener messageListener) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.messageListener = messageListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT || viewType == VIEW_TYPE_SENT_IMAGE || viewType == VIEW_TYPE_SENT_FILE) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageGroupBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setTextData(chatMessages.get(position));
            if (position == chatMessages.size() - 1) {
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.VISIBLE);
            } else {
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.GONE);
            }
            if (chatMessages.get(position).isSeen)
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setText("Seen");
            else
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setText("Delivered");
        } else if (getItemViewType(position) == VIEW_TYPE_RECEIVED) {
            ((ReceivedMessageViewHolder) holder).setTextData(chatMessages.get(position));
        } else if (getItemViewType(position) == VIEW_TYPE_SENT_IMAGE) {
            ((SentMessageViewHolder) holder).setImageData(chatMessages.get(position));
            if (position == chatMessages.size() - 1) {
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.VISIBLE);
            } else {
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.GONE);
            }
            if (chatMessages.get(position).isSeen)
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setText("Seen");
            else
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setText("Delivered");
        } else if (getItemViewType(position) == VIEW_TYPE_RECEIVED_IMAGE) {
            ((ReceivedMessageViewHolder) holder).setImageData(chatMessages.get(position));
        } else if (getItemViewType(position) == VIEW_TYPE_SENT_FILE) {
            ((SentMessageViewHolder) holder).setFileData(chatMessages.get(position));
            if (position == chatMessages.size() - 1) {
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.VISIBLE);
            } else {
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.GONE);
            }
            if (chatMessages.get(position).isSeen)
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setText("Seen");
            else
                ((ChatGroupAdapter.SentMessageViewHolder) holder).binding.textSeenMessage.setText("Delivered");
        } else if (getItemViewType(position) == VIEW_TYPE_RECEIVED_FILE) {
            ((ReceivedMessageViewHolder) holder).setFileData(chatMessages.get(position));
        }

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).type != null) {
            if (chatMessages.get(position).senderId.equals(senderId)) {
                if (chatMessages.get(position).type.equals(Constants.MESSAGE_TEXT)) {
                    return VIEW_TYPE_SENT;
                } else if (chatMessages.get(position).type.equals(Constants.MESSAGE_IMAGE)) {
                    return VIEW_TYPE_SENT_IMAGE;
                } else if (chatMessages.get(position).type.equals(Constants.MESSAGE_FILE)) {
                    return VIEW_TYPE_SENT_FILE;
                }

            } else {
                if (chatMessages.get(position).type.equals(Constants.MESSAGE_IMAGE)) {
                    return VIEW_TYPE_RECEIVED_IMAGE;
                } else if (chatMessages.get(position).type.equals(Constants.MESSAGE_FILE)) {
                    return VIEW_TYPE_RECEIVED_FILE;
                }
            }
        }

        if (chatMessages.get(position).model.equals("receiver")) {
            if (position != 0) {

                if (chatMessages.get(position - 1).model.equals("sender")) {
                    chatMessages.get(position).lastReceiver = true;
                } else if (!Objects.equals(chatMessages.get(position).senderId, chatMessages.get(position - 1).senderId)) {
                    chatMessages.get(position).lastReceiver = true;
                }

            }
        }

        if (chatMessages.get(0).model.equals("receiver"))
            chatMessages.get(0).lastReceiver = true;

        return VIEW_TYPE_RECEIVED;
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
            binding.layoutMessage.setOnClickListener(view -> messageListener.onMessageSelection(chatMessages.get(getAdapterPosition()).isSelected,
                    getAdapterPosition(), chatMessages, chatMessages.get(getAdapterPosition())));
        }

        void setTextData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textMessage.setVisibility(ViewGroup.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);
            preferenceManager = new PreferenceManager(this.itemView.getContext());
            binding.textMessage.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));
        }

        void setImageData(ChatMessage chatMessage) {
            binding.layoutMessage.setBackground(null);
            binding.imageMessage.setImageBitmap(getUserImage(chatMessage.message));
            binding.imageMessage.setVisibility(ViewGroup.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);
        }

        void setFileData(ChatMessage chatMessage) {
            try {
//                URL fileUrl = new URL(chatMessage.message);
                StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(chatMessage.message);
                String fileName = storageReference.getName().toString();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                String name = fileName.split("--__")[0];
                binding.textMessage.setText(name + "." + extension);
                binding.textMessage.setVisibility(ViewGroup.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.downBtn.setVisibility(ViewGroup.VISIBLE);

                binding.downBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        downloadFile(view.getContext(), name + "." + extension, chatMessage.message);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        private void downloadFile(Context context, String fileName, String url) {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/HeyChat");
            if (!folder.exists()) {
                folder.mkdir();
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            request.setDestinationInExternalPublicDir(name, fileName);
//            request.setDestinationInExternalPublicDir("/Download/HeyChat pdf", fileName);

            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/HeyChat/", fileName);
            request.setDestinationUri(Uri.fromFile(file));
            downloadManager.enqueue(request);
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageGroupBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageGroupBinding ItemContainerReceivedMessageGroupBinding) {
            super(ItemContainerReceivedMessageGroupBinding.getRoot());
            binding = ItemContainerReceivedMessageGroupBinding;
            binding.layoutMessage.setOnClickListener(view -> messageListener.onMessageSelection(chatMessages.get(getAdapterPosition()).isSelected,
                    getAdapterPosition(), chatMessages, chatMessages.get(getAdapterPosition())));

        }

        void setTextData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textMessage.setVisibility(View.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);
            preferenceManager = new PreferenceManager(this.itemView.getContext());
            binding.textMessage.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));

            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USER).document(chatMessage.senderId)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        binding.imageProfile.setImageBitmap(getBitmapFromEncodedString(documentSnapshot.getString(Constants.KEY_IMAGE)));
                        binding.textName.setText(documentSnapshot.getString(Constants.KEY_NAME));
                    });

            if (chatMessage.lastReceiver) {
                binding.imageProfile.setVisibility(View.VISIBLE);
                binding.textName.setVisibility(View.VISIBLE);
                // binding.viewSupporter2.setVisibility(View.VISIBLE);
            } else {
                binding.imageProfile.setVisibility(View.GONE);
                binding.textName.setVisibility(View.GONE);
                // binding.viewSupporter2.setVisibility(View.GONE);
            }


        }

        void setImageData(ChatMessage chatMessage) {
            binding.layoutMessage.setBackground(null);
            binding.imageMessage.setImageBitmap(getBitmapFromEncodedString(chatMessage.message));
            binding.imageMessage.setVisibility(View.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);

            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USER).document(chatMessage.senderId)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        binding.imageProfile.setImageBitmap(getBitmapFromEncodedString(documentSnapshot.getString(Constants.KEY_IMAGE)));
                        binding.textName.setText(documentSnapshot.getString(Constants.KEY_NAME));
                    });
        }

        void setFileData(ChatMessage chatMessage) {
            try {
//                URL fileUrl = new URL(chatMessage.message);
                StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(chatMessage.message);
                String fileName = storageReference.getName().toString();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                String name = fileName.split("--__")[0];
                binding.textMessage.setText(name + "." + extension);
                binding.textMessage.setVisibility(ViewGroup.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.downBtn.setVisibility(ViewGroup.VISIBLE);

                binding.downBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        downloadFile(view.getContext(), name + "." + extension, chatMessage.message);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USER).document(chatMessage.senderId)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        binding.imageProfile.setImageBitmap(getBitmapFromEncodedString(documentSnapshot.getString(Constants.KEY_IMAGE)));
                        binding.textName.setText(documentSnapshot.getString(Constants.KEY_NAME));
                    });
        }

        private Bitmap getBitmapFromEncodedString(String encodedImage) {
            if (encodedImage != null) {
                byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else {
                return null;
            }
        }

        private void downloadFile(Context context, String fileName, String url) {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/HeyChat");
            if (!folder.exists()) {
                folder.mkdir();
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            request.setDestinationInExternalPublicDir(name, fileName);
//            request.setDestinationInExternalPublicDir("/Download/HeyChat pdf", fileName);

            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/HeyChat/", fileName);
            request.setDestinationUri(Uri.fromFile(file));
            downloadManager.enqueue(request);
        }

    }

}

