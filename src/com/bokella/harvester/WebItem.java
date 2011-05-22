package com.bokella.harvester;

public class WebItem {
	private String url		= null;
    private String title	= null;
    private String summary	= null;
    private String thumbUrl = null;
    private String status	= null;
	
    public String getThumbUrl() {
		return thumbUrl;
	}
	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String toString() {
		return this.url + ((this.title != null) ? (" '" + this.title + "'") : "") + ((this.thumbUrl != null) ? (" (" + this.thumbUrl + ")") : "") + ((this.summary != null) ? (" - " + this.summary) : "");
	}
	public int hashCode() {
		if (this.url != null) {
			return this.url.hashCode();
		}
		return super.hashCode();
	}
}
