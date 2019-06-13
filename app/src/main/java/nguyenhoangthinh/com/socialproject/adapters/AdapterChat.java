package nguyenhoangthinh.com.socialproject.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nguyenhoangthinh.com.socialproject.R;
import nguyenhoangthinh.com.socialproject.models.Chat;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.Holder> {

    private static final int MESSAGE_TYPE_ON_LEFT = 0;

    private static final int MESSAGE_TYPE_ON_RIGHT = 1;

    private Context mContext;

    private List<Chat> chatList;

    private String imageUrl;

    private FirebaseUser mUser;

    public AdapterChat(Context mContext, List<Chat> chatList, String imageUrl) {
        this.mContext = mContext;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    /*
     * Dựa vào type để tạo ViewHolder tương ứng.
     */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Log.d("MY_TAG","onCreateViewHolder " + i);
        if(i == MESSAGE_TYPE_ON_RIGHT){
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.row_contains_chat_right,viewGroup,false);
            return new Holder(view);
        }else{
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.row_contains_chat_left,viewGroup,false);
            return new Holder(view);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull Holder holder,final int i) {
        Log.d("MY_TAG","onBindViewHolder " + i);
        //get data
        String message = chatList.get(i).getMessage();
        String timestamp = chatList.get(i).getTimestamp();

        //format time
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
            calendar.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime = DateFormat.format("yyyy/MM/dd hh:mm aa",calendar).toString();

        //set data
        holder.txtMessage.setText(message);
        holder.txtTimeSend.setText(dateTime);
        try{
            Picasso.get().load(imageUrl).into(holder.imgProfile);
        }catch (Exception e){

        }

        //handle click message
        holder.messageLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage("Do you want delete this message?");

                //yes button
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessages(i);
                    }
                });

                //no button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return false;
            }
        });

        //set seen/delivered state of message
        //Kiểm tra xem tin nhắn cuối cùng trong danh sách
        //TH1: của row left thì isSeen đã GONE trong xml
        //TH2: của row right thì kiểm tra xem đã nhận được tin nhắn chưa
        if(i == chatList.size() - 1){
            if(chatList.get(i).isSeen()){
                holder.txtIsSeen.setText("Seen");
            }else{
                holder.txtIsSeen.setText("Delivered");
            }
        }else{
            holder.txtIsSeen.setVisibility(View.GONE);
        }
    }

    /**
     * @param position , vị trí cần xóa
     *          Hàm xóa tin nhắn
     */
    private void deleteMessages(int position) {
        final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        /*
         * Logic:
         * Nhận timestamp của message được click
         * So sánh với tất cả message in Chats node
         * Nếu cả hai trùng nhau thì xóa
         */
        String msg = chatList.get(position).getTimestamp();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = reference.orderByChild("timestamp").equalTo(msg);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /* Có 2 cách xóa tin nhắn
                 * Cách 1: Xóa luôn node child trên firebase
                 * Cách 2: Đánh dấu tin nhắn thành đã xóa
                 */
                for(DataSnapshot ds:dataSnapshot.getChildren()){

                    if(ds.child("sender").getValue().equals(myUid)) {
                        //Xóa trên firebase
                        //ds.getRef().removeValue();

                        //Thay đổi nội dung
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "The message has been deleted");
                        ds.getRef().updateChildren(hashMap);

                        Toast.makeText(mContext,"Message deleted...",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(mContext,
                                "You can delete only your message...",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    /*
    * Lấy về type tương ứng với từng position trong collection, position từ cao đến thấp
    */
    @Override
    public int getItemViewType(int position) {
        Log.d("MY_TAG","getItemViewType " + position);
        //Hiện đang đăng nhập người dùng
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(chatList.get(position).getSender().equals(mUser.getUid())){
            return MESSAGE_TYPE_ON_RIGHT;
        }
        return  MESSAGE_TYPE_ON_LEFT;
    }

    //ViewHolder

    class Holder extends RecyclerView.ViewHolder{

        //UI
        ImageView imgProfile;

        TextView txtMessage, txtTimeSend, txtIsSeen;

        LinearLayout messageLinearLayout;

        public Holder(@NonNull View itemView) {
            super(itemView);

            //map views
            imgProfile  = itemView.findViewById(R.id.imageProfile);
            txtMessage  = itemView.findViewById(R.id.txtMessage);
            txtTimeSend = itemView.findViewById(R.id.txtTimeSend);
            txtIsSeen   = itemView.findViewById(R.id.txtSeenMessage);
            messageLinearLayout = itemView.findViewById(R.id.messageLayout);
        }
    }
}
