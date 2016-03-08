/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.blog;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.checks.prefab.Role;
import example.blog.security.IsOwner;
import example.blog.security.IsPublished;
import example.blog.security.IsRevisionOwner;
import example.blog.security.UserAssociatedRecord;
import example.blog.security.VersionedRecord;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Include(rootLevel = true)
@ReadPermission(any = { IsPublished.class })
@UpdatePermission(all = { IsOwner.class })
@DeletePermission(all = { IsOwner.class })
@SharePermission(all = { Role.ALL.class })
public class Post extends BlogObject implements UserAssociatedRecord, VersionedRecord<Post> {
    private boolean published;
    private String title;
    private String text;
    private Blogger author;
    private Post impetus;
    private Post nextRevision;
    private Post previousRevision;
    private Set<Blogger> moderators = new HashSet<>();
    private List<Comment> comments = new ArrayList<>();

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public boolean isPublished() {
        return published;
    }
    public void setPublished(boolean published) {
        this.published = published;
    }

    @ManyToOne
    public Blogger getAuthor() {
        return author;
    }
    public void setAuthor(Blogger author) {
        this.author = author;
    }

    @OneToOne
    @ReadPermission(any = { IsOwner.class })
    public Post getImpetus() {
        return impetus;
    }
    public void setImpetus(Post impetus) {
        this.impetus = impetus;
    }

    @OneToOne(targetEntity = Post.class)
    @ReadPermission(any = { IsOwner.class, IsRevisionOwner.class })
    @UpdatePermission(any = { IsOwner.class, IsRevisionOwner.class })
    public Post getNextRevision() {
        return nextRevision;
    }
    public void setNextRevision(Post nextRevision) {
        this.nextRevision = nextRevision;
    }

    @OneToOne(mappedBy = "nextRevision", targetEntity = Post.class)
    @ReadPermission(any = { IsOwner.class, IsRevisionOwner.class })
    @UpdatePermission(any = { IsOwner.class, IsRevisionOwner.class })
    public Post getPreviousRevision() {
        return previousRevision;
    }
    public void setPreviousRevision(Post previousRevision) {
        this.previousRevision = previousRevision;
    }

    @OneToMany(targetEntity = Comment.class, mappedBy = "post")
    @UpdatePermission(any = { IsPublished.class })
    public List<Comment> getComments() {
        return comments;
    }
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @ManyToMany(targetEntity = Blogger.class)
    @ReadPermission(any = { IsOwner.class })
    public Set<Blogger> getModerators() {
        return moderators;
    }
    public void setModerators(Set<Blogger> moderators) {
        this.moderators = moderators;
    }

    @Override
    public Blogger getUser() {
        return author;
    }
}
