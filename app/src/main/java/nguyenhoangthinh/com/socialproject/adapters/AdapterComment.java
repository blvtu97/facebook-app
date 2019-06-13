package nguyenhoangthinh.com.socialproject.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import nguyenhoangthinh.com.socialproject.R;
import nguyenhoangthinh.com.socialproject.models.Comment;
import nguyenhoangthinh.com.socialproject.services.SocialNetwork;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.Holder> {

    private Context mContext;

    private List<Comment> commentList;

    //ID của bài viết
    private String pId;

    public AdapterComment(Context mContext, List<Comment> commentList) {
        this.mContext = mContext;
        this.commentList = commentList;
    }

    public List<Comment> getCommentList() {
        return commentList;
    }

    public void setCommentList(List<Comment> commentList) {
        this.commentList = commentList;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //Tạo row news feed

        View view = LayoutInflater.from(mContext).inflate(R.layout.row_comment,
                viewGroup, false);
        return new Holder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, final int i) {
       //Nếu là row dùng để comment
        if (i == (commentList.size() - 1)) {

            holder.txtName.setVisibility(View.GONE);
            holder.btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(holder.edtComment.getText().toString().trim().equals("")){
                        Toast.makeText(mContext,
                                "Content is not allowed to be empty!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SocialNetwork.commentForPost(pId, holder.edtComment.getText().toString());
                    holder.edtComment.setText("");
                }
            });
        }else{

            holder.btnSend.setVisibility(View.GONE);
            holder.txtTime.setVisibility(View.VISIBLE);
            //format time
            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
            calendar.setTimeInMillis(Long.parseLong(commentList.get(i).getcTime()));
            String dateTime =
                    DateFormat.format("yyyy/MM/dd hh:mm aa",calendar).toString();
            String id = commentList.get(i).getUid();
            holder.txtTime.setText(dateTime);
            holder.edtComment.setText(commentList.get(i).getcContent());
            holder.edtComment.setEnabled(false);

            holder.txtName.setText(SocialNetwork.findNameById(id));
            try {
                Picasso.get().load(SocialNetwork.findImageById(id))
                        .placeholder(R.drawable.ic_user_anonymous).into(holder.imgProfile);
            }catch (Exception e){

            }
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    //View holder
    class Holder extends RecyclerView.ViewHolder{

        //Nhóm views của người dùng đăng nhập hiện tại để comment
        CircleImageView imgProfile;

        EditText edtComment;

        TextView txtName, txtTime;

        ImageButton btnSend;
        //

        public Holder(@NonNull View itemView) {
            super(itemView);

            //init views
            imgProfile    = itemView.findViewById(R.id.imgProfileComment);
            txtName       = itemView.findViewById(R.id.txtNameComment);
            txtTime       = itemView.findViewById(R.id.txtTimeComment);
            edtComment    = itemView.findViewById(R.id.edtComment);
            btnSend       = itemView.findViewById(R.id.btnSendComment);
        }
    }

}
