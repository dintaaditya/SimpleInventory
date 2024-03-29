package com.dintaaditya.simpleinventory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dintaaditya.simpleinventory.Model.ItemLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import javax.annotation.Nullable;

public class DetailActivity extends AppCompatActivity implements View.OnClickListener {
    String SKU;
    ImageView imgItem;
    EditText edtSKU, edtName, edtStock;
    Button btnShowLog, btnSendItem, btnCancel, btnUpdate;
    DocumentReference itemDetail;
    CollectionReference logItemRef;
    int previous_stock, stock;

    LinearLayout progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        SKU = getIntent().getStringExtra("SKU");

        imgItem = findViewById(R.id.img_item);
        edtSKU = findViewById(R.id.edt_SKU);
        edtName = findViewById(R.id.edt_name);
        edtStock = findViewById(R.id.edt_stock);

        btnShowLog = findViewById(R.id.btn_log_item);
        btnSendItem = findViewById(R.id.btn_send_item);
        btnCancel = findViewById(R.id.btn_cancel);
        btnUpdate = findViewById(R.id.btn_update);
        progressBar = findViewById(R.id.progress_bar);

        btnShowLog.setOnClickListener(this);
        btnSendItem.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);

        formState(false);
        itemDetail = FirebaseFirestore.getInstance().document("Item/" + SKU);
        logItemRef = FirebaseFirestore.getInstance().collection("Item/" + SKU + "/Log");


    }

    @Override
    protected void onStart() {
        super.onStart();
        showItemDetail();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_log_item:
                startActivity(new Intent(DetailActivity.this, ItemLogActivity.class).putExtra("SKU", SKU));
                break;
            case R.id.btn_send_item:
                startActivity(new Intent(DetailActivity.this, ShipmentActivity.class).putExtra("SKU", SKU));
                break;
            case R.id.btn_cancel:
                showItemDetail();
                formState(false);
                break;
            case R.id.btn_update:
                closeKeyboard();
                progressBar.setVisibility(View.VISIBLE);
                updateData();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.detail_edit:
                formState(true);
                break;
            case R.id.detail_delete:
                deleteData();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showItemDetail() {

        itemDetail.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String name = documentSnapshot.getString("name");
                String image = documentSnapshot.getString("image");
                stock = documentSnapshot.getLong("stock").intValue();
                edtSKU.setText(SKU);
                edtName.setText(name);
                edtStock.setText(String.valueOf(stock));
                Glide.with(DetailActivity.this).load(image).apply(RequestOptions.circleCropTransform()).into(imgItem);
            }
        });
    }

    private void updateData() {
        String name = edtName.getText().toString().trim();
        final int last_stock = Integer.parseInt(edtStock.getText().toString());
        previous_stock = stock;
        itemDetail.update(
                "name", name,
                "stock", last_stock)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        String status;
                        int stock_movement;
                        if (last_stock > previous_stock) {
                            status = "Incoming";
                            stock_movement = last_stock - previous_stock;
                        } else {
                            status = "Outcoming";
                            stock_movement = previous_stock - last_stock;
                        }
                        ItemLog newLog = new ItemLog(previous_stock, stock_movement, last_stock, status);
                        logItemRef.add(newLog)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(DetailActivity.this, "New Log Added Successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(DetailActivity.this, "Failed to add Log" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailActivity.this, "Data Updated Successfully", Toast.LENGTH_SHORT).show();
                        formState(false);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DetailActivity.this, "Failed to update data!!, " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Data")
                .setMessage("Do you want to delete this data?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        itemDetail.delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        startActivity(new Intent(getApplicationContext(), ItemActivity.class));
                                        finish();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(DetailActivity.this, "Failed to delete data!!, " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                })
                .setNegativeButton("NO", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void formState(boolean status) {
        edtName.setEnabled(status);
        edtStock.setEnabled(status);
        if (status) {
            btnCancel.setVisibility(View.VISIBLE);
            btnUpdate.setVisibility(View.VISIBLE);
            btnShowLog.setVisibility(View.GONE);
            btnSendItem.setVisibility(View.GONE);
        } else {
            btnCancel.setVisibility(View.GONE);
            btnUpdate.setVisibility(View.GONE);
            btnShowLog.setVisibility(View.VISIBLE);
            btnSendItem.setVisibility(View.VISIBLE);
        }
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager i = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            i.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
