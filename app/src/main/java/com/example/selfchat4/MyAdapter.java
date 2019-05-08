package com.example.selfchat4;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    int local_id = 0;

    public static final String TIME_FORMAT = "kk:mm";
    public static final String GLOBAL_ID_DOCUMENT_ID = "jrgeC8ZrtOgGY1bOpALJ";
    public static final String GLOBAL_ID_FIELD_NAME = "project_id";
    public static final String COLLECTION_NAME = "messages";

    public static final String MESSAGE_CONTENT_FIELD = "content";
    public static final String MESSAGE_ID_FIELD = "id";
    public static final String MESSAGE_TIMESTAMP = "timestamp";

    public ArrayList<Message> data;

    private recItemOnLongClick clickedMessage;
    public int data_size;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private FirebaseFirestore db;

    public interface recItemOnLongClick {
        void itemLongClick(View view, final int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{
        TextView textView;
        TextView timestamp;
        public MyViewHolder(View view){
            super(view);
            textView = view.findViewById(R.id.textView_one_message_template);
            timestamp = view.findViewById(R.id.timestamp);
            view.setOnLongClickListener(this);
        }

        public boolean onLongClick(View view){
            if (clickedMessage != null) {
                clickedMessage.itemLongClick(view, getAdapterPosition());
            }
            return true;
        }
    }

    public MyAdapter(int size, SharedPreferences other_sp, SharedPreferences.Editor other_editor,
                     FirebaseFirestore db){
        this.data = new ArrayList<Message>();
        this.data_size = size;
        this.sp = other_sp;
        this.editor = other_editor;
        this.gson = new Gson();
        this.db = db;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.view_one_message, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String message = data.get(position).Content;
        String timestamp = data.get(position).Timestamp;
        holder.textView.setText(message);
        holder.timestamp.setText(timestamp);
    }

    @Override
    public int getItemCount() {
        if(data == null)
        {
            data = new ArrayList<>();
            return 0;
        }
        return data.size();
    }

    public void add_message(String id, String timestamp, String message){
        this.data.add(new Message(id, timestamp, message));
        data_size += 1;
        saveEditedData();
        notifyDataSetChanged();
    }

    public void delete_message(int position){
        new DeleteDataFromFireBase().execute(this.data.get(position).Id);
        this.data.remove(position);
        data_size -= 1;
        saveEditedData();
        notifyItemRemoved(position);
    }

    public void saveEditedData() {
        editor.putInt(MainActivity.SP_DATA_SIZE_KEY, this.data_size);
        String wjson = gson.toJson(this.data);
        editor.putString(MainActivity.SP_DATA_LIST_KEY, wjson);
        editor.apply();
    }

    public void setClickListener(recItemOnLongClick itemClick) {
        this.clickedMessage = itemClick;
    }

    public void loadData() {
        String rjson = sp.getString(MainActivity.SP_DATA_LIST_KEY, "");
        Type type = new TypeToken<List<Message>>() {
        }.getType();
        this.data = gson.fromJson(rjson, type);
    }

    public static String getTime() {
        DateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);
        return dateFormat.format(new Date());
    }

    public void supportConfigurationChange()
    {
        notifyDataSetChanged();
    }


    /*---------------------------  INSERT DATA TO FIRE BASE ---------------------------*/
    public void addToRemoteFireBase(final String message)
    {
        String currentTime = getTime();
        incrementGlobalId(local_id);
        addDocument(local_id, message, currentTime);
    }

    public void addDocument(final int id, final String message, String currentTime)
    {
        Map<String, Object> sent_message = new HashMap<>();
        int increment_id = id + 1;

        sent_message.put(MESSAGE_CONTENT_FIELD, message);
        sent_message.put(MESSAGE_TIMESTAMP,currentTime);
        sent_message.put(MESSAGE_ID_FIELD, increment_id);

        db.collection(COLLECTION_NAME)
                .document(increment_id + "")
                .set(sent_message)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(" ", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(" ", "Error writing document", e);
                    }
                });
        local_id++;
    }

    /*---------------------------  INCREMENT GLOBAL ID TO FIRE BASE ---------------------------*/
    public void incrementGlobalId(int id)
    {
        DocumentReference washingtonRef = db.collection(COLLECTION_NAME).
                document(GLOBAL_ID_DOCUMENT_ID);

        washingtonRef
                .update(GLOBAL_ID_FIELD_NAME, id + 1)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("", "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("", "Error updating document", e);
                    }
                });
    }

    /*---------------------------  SELECT GLOBAL ID FROM FIRE BASE ---------------------------*/
    public void getGlobalId()
    {
        DocumentReference docRef = db.collection(COLLECTION_NAME).
                document(GLOBAL_ID_DOCUMENT_ID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        final String id = document.getData().get(GLOBAL_ID_FIELD_NAME) + "";
                        local_id = Integer.parseInt(id);
                    } else {
                        Log.d("", "No such document");
                    }
                } else {
                    Log.d("", "get failed with ", task.getException());
                }
            }
        });
    }

    /*---------------------------  DELETE DATA FROM FIRE BASE ---------------------------*/
    public void deleteDocument(String doc_id)
    {
        db.collection(COLLECTION_NAME).document(doc_id)
                .delete()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(" ", "Error deleting document", e);
                    }
                });
    }

    /*---------------------------  SELECT ALL DATA FROM FIRE BASE ---------------------------*/
    public void loadDataFromRemoteFireBase()
    {
        final ArrayList<Message> d = new ArrayList<Message>();
        db.collection(COLLECTION_NAME)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            String id, timestamp, content;
                            Map<String, Object> one_message;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if(!document.getId().equals(GLOBAL_ID_DOCUMENT_ID))
                                {
                                    one_message = document.getData();
                                    id = one_message.get(MESSAGE_ID_FIELD) + "";
                                    timestamp = one_message.get(MESSAGE_TIMESTAMP) + "";
                                    content = one_message.get(MESSAGE_CONTENT_FIELD) + "";
                                    d.add(new Message(id, timestamp, content));
                                }
                            }

                            for (Message m: d)
                                add_message(m.Id, m.Timestamp, m.Content);
                            loadData();

                        } else {
                            Log.d(" ", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    /*------------------------  UI BACKGROUND THREAD ACTIVATES DELETE ------------------------*/
    private class DeleteDataFromFireBase extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            deleteDocument(strings[0]);
            return null;
        }
    }
}
