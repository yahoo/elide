#!/bin/bash
# Example that exercises security features in Elide

# Entities:
# Post
#
# - Author (User)
# - Date Posted
# - Content (String)
# - Comments (Collection)
#
# User
#
# - Name
# - Email
# - Type (Admin, Registered, Unregistered)
#
# Comment
#
# - Author (User)
# - Date Posted
# - Content (String)
#
# Relationships:
# Post -1> User
# Post -oo> Comment
# Comment -1> User
#
# Security:
#
# - Admin User can:
#     - Do anything
# - Registered User can:
#     - Create post
#     - Edit their post
#     - Comment on a post
#     - Edit their comment
#     - Delete their comment
# - Registered User cannot:
#     - Delete their post
#     - Delete comments on their post
#     - Edit other people’s comments
#     - Delete other people’s comments
# - Unregistered User can:
#     - Read posts
# - Unregistered User cannot:
#     - Create posts
#     - Create comments
#     - Read comments
#     - Delete anything
#     - Edit anything

# Allowed: Create user 1
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "user",
    "attributes": {
      "name": "Michael Admin",
      "role": "Admin"
    }
  }
}
' -X POST http://localhost:4080/user

# Allowed: Create user 2
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "user",
    "attributes": {
      "name": "Ken Registered",
      "role": "Registered"
    }
  }
}
' -X POST http://localhost:4080/user

# Allowed: Create user 3
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "user",
    "attributes": {
      "name": "Shad Unreg",
      "role": "Unregistered"
    }
  }
}
' -X POST http://localhost:4080/user

# Allowed: Create post 1 - admin (me=1) creates a post:
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "post",
    "attributes": {
      "content": "The greatest thing ever by Michael",
      "me": "1"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "user",
          "id": "1"
        }
      }
    }
  }
}
' -X POST http://localhost:4080/post

# Allowed: Create post 2 - registered user (me=2) creates a post:
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "post",
    "attributes": {
      "content": "The 2nd best thing ever by Ken",
      "me": "2"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "user",
          "id": "2"
        }
      }
    }
  }
}
' -X POST http://localhost:4080/post

# Not Allowed: Create post - registered user (me=2) creates a post that doesn't belong to me
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "post",
    "attributes": {
      "content": "Ken tries to put words into Shads mouth",
      "me": "2"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "user",
          "id": "3"
        }
      }
    }
  }
}
' -X POST http://localhost:4080/post

# Not Allowed: Create post - unregistered user (me=3) creates a post
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "post",
    "attributes": {
      "content": "Shad tries to post",
      "me": "3"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "user",
          "id": "3"
        }
      }
    }
  }
}
' -X POST http://localhost:4080/post

# Allowed: Michael Admin comments on Ken's Post
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "comment",
    "attributes": {
      "content": "That is truly the 2nd best post ever",
      "me": "1"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "user",
          "id": "1"
        }
      }
    }
  }
}
' -X POST http://localhost:4080/post/2/comments

# Not Allowed: Shad Unreg comments on Ken Reg's Post
curl -H'Content-Type: application/vnd.api+json' \
     -H'Accept: application/vnd.api+json' --data '
{
  "data": {
    "type": "comment",
    "attributes": {
      "content": "I think this is the 3rd best post ever",
      "me": "3"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "user",
          "id": "3"
        }
      }
    }
  }
}
' -X POST http://localhost:4080/post/2/comments
