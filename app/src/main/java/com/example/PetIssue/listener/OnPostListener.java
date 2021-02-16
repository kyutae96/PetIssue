package com.example.PetIssue.listener;

import com.example.PetIssue.PostInfo;

public interface OnPostListener {
    void onDelete(PostInfo postInfo);
    void onModify();
}
