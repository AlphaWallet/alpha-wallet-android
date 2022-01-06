package com.alphawallet.app.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GitHubRelease {

    public class Asset {

        public class Uploader {

            @SerializedName("login")
            @Expose
            private String login;
            @SerializedName("id")
            @Expose
            private Integer id;
            @SerializedName("node_id")
            @Expose
            private String nodeId;
            @SerializedName("avatar_url")
            @Expose
            private String avatarUrl;
            @SerializedName("gravatar_id")
            @Expose
            private String gravatarId;
            @SerializedName("url")
            @Expose
            private String url;
            @SerializedName("html_url")
            @Expose
            private String htmlUrl;
            @SerializedName("followers_url")
            @Expose
            private String followersUrl;
            @SerializedName("following_url")
            @Expose
            private String followingUrl;
            @SerializedName("gists_url")
            @Expose
            private String gistsUrl;
            @SerializedName("starred_url")
            @Expose
            private String starredUrl;
            @SerializedName("subscriptions_url")
            @Expose
            private String subscriptionsUrl;
            @SerializedName("organizations_url")
            @Expose
            private String organizationsUrl;
            @SerializedName("repos_url")
            @Expose
            private String reposUrl;
            @SerializedName("events_url")
            @Expose
            private String eventsUrl;
            @SerializedName("received_events_url")
            @Expose
            private String receivedEventsUrl;
            @SerializedName("type")
            @Expose
            private String type;
            @SerializedName("site_admin")
            @Expose
            private Boolean siteAdmin;

            public String getLogin() {
                return login;
            }

            public void setLogin(String login) {
                this.login = login;
            }

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public String getNodeId() {
                return nodeId;
            }

            public void setNodeId(String nodeId) {
                this.nodeId = nodeId;
            }

            public String getAvatarUrl() {
                return avatarUrl;
            }

            public void setAvatarUrl(String avatarUrl) {
                this.avatarUrl = avatarUrl;
            }

            public String getGravatarId() {
                return gravatarId;
            }

            public void setGravatarId(String gravatarId) {
                this.gravatarId = gravatarId;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getHtmlUrl() {
                return htmlUrl;
            }

            public void setHtmlUrl(String htmlUrl) {
                this.htmlUrl = htmlUrl;
            }

            public String getFollowersUrl() {
                return followersUrl;
            }

            public void setFollowersUrl(String followersUrl) {
                this.followersUrl = followersUrl;
            }

            public String getFollowingUrl() {
                return followingUrl;
            }

            public void setFollowingUrl(String followingUrl) {
                this.followingUrl = followingUrl;
            }

            public String getGistsUrl() {
                return gistsUrl;
            }

            public void setGistsUrl(String gistsUrl) {
                this.gistsUrl = gistsUrl;
            }

            public String getStarredUrl() {
                return starredUrl;
            }

            public void setStarredUrl(String starredUrl) {
                this.starredUrl = starredUrl;
            }

            public String getSubscriptionsUrl() {
                return subscriptionsUrl;
            }

            public void setSubscriptionsUrl(String subscriptionsUrl) {
                this.subscriptionsUrl = subscriptionsUrl;
            }

            public String getOrganizationsUrl() {
                return organizationsUrl;
            }

            public void setOrganizationsUrl(String organizationsUrl) {
                this.organizationsUrl = organizationsUrl;
            }

            public String getReposUrl() {
                return reposUrl;
            }

            public void setReposUrl(String reposUrl) {
                this.reposUrl = reposUrl;
            }

            public String getEventsUrl() {
                return eventsUrl;
            }

            public void setEventsUrl(String eventsUrl) {
                this.eventsUrl = eventsUrl;
            }

            public String getReceivedEventsUrl() {
                return receivedEventsUrl;
            }

            public void setReceivedEventsUrl(String receivedEventsUrl) {
                this.receivedEventsUrl = receivedEventsUrl;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Boolean getSiteAdmin() {
                return siteAdmin;
            }

            public void setSiteAdmin(Boolean siteAdmin) {
                this.siteAdmin = siteAdmin;
            }

        }

        @SerializedName("url")
        @Expose
        private String url;
        @SerializedName("id")
        @Expose
        private Integer id;
        @SerializedName("node_id")
        @Expose
        private String nodeId;
        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("label")
        @Expose
        private Object label;
        @SerializedName("uploader")
        @Expose
        private Uploader uploader;
        @SerializedName("content_type")
        @Expose
        private String contentType;
        @SerializedName("state")
        @Expose
        private String state;
        @SerializedName("size")
        @Expose
        private Integer size;
        @SerializedName("download_count")
        @Expose
        private Integer downloadCount;
        @SerializedName("created_at")
        @Expose
        private String createdAt;
        @SerializedName("updated_at")
        @Expose
        private String updatedAt;
        @SerializedName("browser_download_url")
        @Expose
        private String browserDownloadUrl;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getLabel() {
            return label;
        }

        public void setLabel(Object label) {
            this.label = label;
        }

        public Uploader getUploader() {
            return uploader;
        }

        public void setUploader(Uploader uploader) {
            this.uploader = uploader;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(Integer downloadCount) {
            this.downloadCount = downloadCount;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getBrowserDownloadUrl() {
            return browserDownloadUrl;
        }

        public void setBrowserDownloadUrl(String browserDownloadUrl) {
            this.browserDownloadUrl = browserDownloadUrl;
        }

    }

    public class Author {

        @SerializedName("login")
        @Expose
        private String login;
        @SerializedName("id")
        @Expose
        private Integer id;
        @SerializedName("node_id")
        @Expose
        private String nodeId;
        @SerializedName("avatar_url")
        @Expose
        private String avatarUrl;
        @SerializedName("gravatar_id")
        @Expose
        private String gravatarId;
        @SerializedName("url")
        @Expose
        private String url;
        @SerializedName("html_url")
        @Expose
        private String htmlUrl;
        @SerializedName("followers_url")
        @Expose
        private String followersUrl;
        @SerializedName("following_url")
        @Expose
        private String followingUrl;
        @SerializedName("gists_url")
        @Expose
        private String gistsUrl;
        @SerializedName("starred_url")
        @Expose
        private String starredUrl;
        @SerializedName("subscriptions_url")
        @Expose
        private String subscriptionsUrl;
        @SerializedName("organizations_url")
        @Expose
        private String organizationsUrl;
        @SerializedName("repos_url")
        @Expose
        private String reposUrl;
        @SerializedName("events_url")
        @Expose
        private String eventsUrl;
        @SerializedName("received_events_url")
        @Expose
        private String receivedEventsUrl;
        @SerializedName("type")
        @Expose
        private String type;
        @SerializedName("site_admin")
        @Expose
        private Boolean siteAdmin;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getGravatarId() {
            return gravatarId;
        }

        public void setGravatarId(String gravatarId) {
            this.gravatarId = gravatarId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }

        public String getFollowersUrl() {
            return followersUrl;
        }

        public void setFollowersUrl(String followersUrl) {
            this.followersUrl = followersUrl;
        }

        public String getFollowingUrl() {
            return followingUrl;
        }

        public void setFollowingUrl(String followingUrl) {
            this.followingUrl = followingUrl;
        }

        public String getGistsUrl() {
            return gistsUrl;
        }

        public void setGistsUrl(String gistsUrl) {
            this.gistsUrl = gistsUrl;
        }

        public String getStarredUrl() {
            return starredUrl;
        }

        public void setStarredUrl(String starredUrl) {
            this.starredUrl = starredUrl;
        }

        public String getSubscriptionsUrl() {
            return subscriptionsUrl;
        }

        public void setSubscriptionsUrl(String subscriptionsUrl) {
            this.subscriptionsUrl = subscriptionsUrl;
        }

        public String getOrganizationsUrl() {
            return organizationsUrl;
        }

        public void setOrganizationsUrl(String organizationsUrl) {
            this.organizationsUrl = organizationsUrl;
        }

        public String getReposUrl() {
            return reposUrl;
        }

        public void setReposUrl(String reposUrl) {
            this.reposUrl = reposUrl;
        }

        public String getEventsUrl() {
            return eventsUrl;
        }

        public void setEventsUrl(String eventsUrl) {
            this.eventsUrl = eventsUrl;
        }

        public String getReceivedEventsUrl() {
            return receivedEventsUrl;
        }

        public void setReceivedEventsUrl(String receivedEventsUrl) {
            this.receivedEventsUrl = receivedEventsUrl;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getSiteAdmin() {
            return siteAdmin;
        }

        public void setSiteAdmin(Boolean siteAdmin) {
            this.siteAdmin = siteAdmin;
        }

    }

    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("assets_url")
    @Expose
    private String assetsUrl;
    @SerializedName("upload_url")
    @Expose
    private String uploadUrl;
    @SerializedName("html_url")
    @Expose
    private String htmlUrl;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("author")
    @Expose
    private Author author;
    @SerializedName("node_id")
    @Expose
    private String nodeId;
    @SerializedName("tag_name")
    @Expose
    private String tagName;
    @SerializedName("target_commitish")
    @Expose
    private String targetCommitish;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("draft")
    @Expose
    private Boolean draft;
    @SerializedName("prerelease")
    @Expose
    private Boolean prerelease;
    @SerializedName("created_at")
    @Expose
    private String createdAt;
    @SerializedName("published_at")
    @Expose
    private String publishedAt;
    @SerializedName("assets")
    @Expose
    private List<Asset> assets = null;
    @SerializedName("tarball_url")
    @Expose
    private String tarballUrl;
    @SerializedName("zipball_url")
    @Expose
    private String zipballUrl;
    @SerializedName("body")
    @Expose
    private String body;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAssetsUrl() {
        return assetsUrl;
    }

    public void setAssetsUrl(String assetsUrl) {
        this.assetsUrl = assetsUrl;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTargetCommitish() {
        return targetCommitish;
    }

    public void setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

    public Boolean getPrerelease() {
        return prerelease;
    }

    public void setPrerelease(Boolean prerelease) {
        this.prerelease = prerelease;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public String getTarballUrl() {
        return tarballUrl;
    }

    public void setTarballUrl(String tarballUrl) {
        this.tarballUrl = tarballUrl;
    }

    public String getZipballUrl() {
        return zipballUrl;
    }

    public void setZipballUrl(String zipballUrl) {
        this.zipballUrl = zipballUrl;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}


