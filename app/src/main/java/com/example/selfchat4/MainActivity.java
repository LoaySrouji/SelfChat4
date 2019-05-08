package com.example.selfchat4;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nullable;


public class MainActivity extends AppCompatActivity implements MyAdapter.recItemOnLongClick{

    public static final String SP_DATA_SIZE_KEY = "data_size";
    public static final String SP_DATA_LIST_KEY = "sent_messages";
    public static final String SP_SYNC_FLAG = "first_launch";

    Button button;
    EditText editText;
    RecyclerView recyclerView;

    MyAdapter myAdapter;

    SharedPreferences sp;
    SharedPreferences.Editor editor;

    FirebaseFirestore db;
    CollectionReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button3);
        editText = (EditText) findViewById(R.id.editText2);
        recyclerView = (RecyclerView) findViewById(R.id.rec1);

        FirebaseApp.initializeApp(MainActivity.this);
        db = FirebaseFirestore.getInstance();
        reference = db.collection(myAdapter.COLLECTION_NAME);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sp.edit();

        int data_size = sp.getInt(SP_DATA_SIZE_KEY, 0);

        myAdapter = new MyAdapter(data_size, sp, editor, db);
        myAdapter.setClickListener(this);

        new getFireBaseId().execute();

        recyclerView.setAdapter(myAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        if(data_size != 0 ) { myAdapter.loadData(); }
        else if (sp.getBoolean(SP_SYNC_FLAG, true))
        {
            new syncLocalToRemoteFireBase().execute();
            editor.putBoolean(SP_SYNC_FLAG, false);
            editor.apply();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = editText.getText().toString();
                editText.setText("");
                if(message.equals(""))
                {
                    Toast.makeText(getApplicationContext(),
                            "you can't send an empty message, oh silly!", Toast.LENGTH_LONG).show();
                    return;
                }
                new insertDataToFireBase().execute(message);
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        myAdapter.data = new ArrayList<Message>();
        reference.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                @Nullable FirebaseFirestoreException e) {
                if(e != null)
                {
                    return;
                }
                for(DocumentChange documentChange: queryDocumentSnapshots.getDocumentChanges())
                {
                    DocumentSnapshot documentSnapshot = documentChange.getDocument();
                    String id = documentSnapshot.getId();
                    boolean isDocumentDeleted = documentChange.getOldIndex() != -1;
                    boolean isDocumentAdded = documentChange.getNewIndex() != -1;
                    if (isDocumentDeleted)
                    {
                        for(int index = 0 ; index < myAdapter.data.size(); index++)
                            if (myAdapter.data.get(index).Id.equals(id)) {
                                myAdapter.delete_message(index);
                                break;
                            }
                    }

                    else if(isDocumentAdded && !
                            documentSnapshot.getId().equals(myAdapter.GLOBAL_ID_DOCUMENT_ID))
                    {
                        Map<String, Object> new_doc_data = documentSnapshot.getData();
                        String Id = new_doc_data.get(myAdapter.MESSAGE_ID_FIELD)+"";
                        String content = new_doc_data.get(myAdapter.MESSAGE_CONTENT_FIELD)+"";
                        String timestamp = new_doc_data.get(myAdapter.MESSAGE_TIMESTAMP)+"";
                        myAdapter.add_message(Id, timestamp, content);
                    }
                }
            }
        });
    }

    @Override
    public void itemLongClick(View view, final int position) {

        new AlertDialog.Builder(this)
                .setTitle("Delete the selected message?")
                .setMessage("Note that you can't restore deleted messages")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        myAdapter.delete_message(position);
                        Toast.makeText(getApplicationContext(),
                                "message was deleted successfully", Toast.LENGTH_LONG).show();

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getApplicationContext(),
                                "deleting process was aborted", Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("wrote_message", editText.getText().toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String wrote_message = savedInstanceState.getString("wrote_message");
        editText.setText(wrote_message);
        myAdapter.loadData();
        myAdapter.supportConfigurationChange();
    }

    /*-------------------  UI BACKGROUND THREAD ACTIVATES READ FROM FIRE BASE -------------------*/
    private class getFireBaseId extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            myAdapter.getGlobalId();
            return null;
        }
    }

    /*------------------------  UI BACKGROUND THREAD ACTIVATES INSERTION ------------------------*/
    private class insertDataToFireBase extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            myAdapter.addToRemoteFireBase(strings[0]);
            return null;
        }
    }

    /*---------------------  UI BACKGROUND THREAD ACTIVATES SYNCHRONIZATION ---------------------*/
    public class syncLocalToRemoteFireBase extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            myAdapter.loadDataFromRemoteFireBase();
            return null;
        }
    }


}
