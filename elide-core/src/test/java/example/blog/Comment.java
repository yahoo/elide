/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.blog;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import example.blog.security.IsOwner;
import example.blog.security.IsPostOwner;
import example.blog.security.PostRelatedRecord;
import example.blog.security.UserAssociatedRecord;

import javax.persistence.Entity;

@Entity
@Include
@ReadPermission(any = { IsOwner.class, IsPostOwner.class })
@UpdatePermission(all = { IsOwner.class })
@SharePermission(any = { IsOwner.class, IsPostOwner.class })
public class Comment extends BlogObject implements PostRelatedRecord, UserAssociatedRecord {
    private Blogger author;
    private Post post;
    private String text;

    public Blogger getAuthor() {
        return author;
    }
    public void setAuthor(Blogger author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    @UpdatePermission(any = { IsOwner.class, IsPostOwner.class })
    public Post getPost() {
        return post;
    }
    public void setPost(Post post) {
        this.post = post;
    }

    @Override
    public Blogger getUser() {
        return author;
    }
}
