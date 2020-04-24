package michael.wilson.geochat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

/** Used to create a list of 'comment_layout' views for a array of 'Comment' objects */
public class CommentListAdapter extends ArrayAdapter<Comment>
{
    // context for housekeeping
    private Context context;

    // The id of the view to be used as a list
    private int resourceId;

    /** Constructor, intializes the adapter
     * > Stores a list of 'Comment' objects and housekeeping info
     *
     * @param _context - Context for housekeeping.
     * @param _resource - The ListView to be filled
     * @param _objects - The list of 'Comment' objects to be displayed
     * */
    public CommentListAdapter(Context _context, int _resource, ArrayList<Comment> _objects)
    {
        super(_context, _resource, _objects);
        this.context = _context;
        this.resourceId = _resource;
    }

    /** Used internally by android to create a listview based on a list of comments */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        Comment tmpComment = getItem(position);
        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resourceId, parent, false);
        TextView commentView = convertView.findViewById(R.id.commentTextView);
        TextView dateView = convertView.findViewById(R.id.commentDateView);
        commentView.setText(tmpComment.getCommentText());
        dateView.setText(tmpComment.getCommentDate());
        return convertView;
    }
}