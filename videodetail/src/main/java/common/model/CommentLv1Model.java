package common.model;

import androidx.annotation.Keep;

import com.chad.library.adapter.base.entity.AbstractExpandableItem;
import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.util.ArrayList;
import java.util.List;

@Keep
public class CommentLv1Model {
    private String code;
    private String success;
    private String message;
    private String detail;
    private DataDTO data;
    private String time;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String isSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Keep
    public static class DataDTO {
        private String total;
        private List<RecordsDTO> records;
        private String pageIndex;
        private String pageSize;

        public String getTotal() {
            return total;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        public List<RecordsDTO> getRecords() {
            return records;
        }

        public void setRecords(List<RecordsDTO> records) {
            this.records = records;
        }

        public String getPageIndex() {
            return pageIndex;
        }

        public void setPageIndex(String pageIndex) {
            this.pageIndex = pageIndex;
        }

        public String getPageSize() {
            return pageSize;
        }

        public void setPageSize(String pageSize) {
            this.pageSize = pageSize;
        }

        @Keep
        public static class RecordsDTO extends AbstractExpandableItem<ReplyLv2Model.ReplyListDTO> implements MultiItemEntity {
            @Override
            public int getLevel() {
                return 1;
            }

            @Override
            public int getItemType() {
                return 1;
            }

            private String id;
            private String contentId;
            private String userId;
            private String content;
            private String createTime;
            private String createBy;
            private String title;
            private String editor;
            private String nickname;
            private String head;
            private String timeDif;
            private String issueTimeStamp;
            private Object children;
            private String isTop;
            private String score;
            private String prizeId;
            private String prizeOrderId;
            private String onShelve;
            private ReplyLv2Model reply;
            private String rnikeName;
            private String rcommentId;
            private String pcommentId;
            private String ruserId;
            private String official;
            private int position;
            private boolean isShow;
            private List<ReplyLv2Model.ReplyListDTO> replyLv2CacheList = new ArrayList<>();
            private List<ReplyLv2Model.ReplyListDTO> replyLv2Alllist = new ArrayList<>();

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getContentId() {
                return contentId;
            }

            public void setContentId(String contentId) {
                this.contentId = contentId;
            }

            public String getUserId() {
                return userId;
            }

            public void setUserId(String userId) {
                this.userId = userId;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public String getCreateTime() {
                return createTime;
            }

            public void setCreateTime(String createTime) {
                this.createTime = createTime;
            }

            public String getCreateBy() {
                return createBy;
            }

            public void setCreateBy(String createBy) {
                this.createBy = createBy;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getEditor() {
                return editor;
            }

            public void setEditor(String editor) {
                this.editor = editor;
            }

            public String getNickname() {
                return nickname;
            }

            public void setNickname(String nickname) {
                this.nickname = nickname;
            }

            public String getHead() {
                return head;
            }

            public void setHead(String head) {
                this.head = head;
            }

            public String getTimeDif() {
                return timeDif;
            }

            public void setTimeDif(String timeDif) {
                this.timeDif = timeDif;
            }

            public String getIssueTimeStamp() {
                return issueTimeStamp;
            }

            public void setIssueTimeStamp(String issueTimeStamp) {
                this.issueTimeStamp = issueTimeStamp;
            }

            public Object getChildren() {
                return children;
            }

            public void setChildren(Object children) {
                this.children = children;
            }

            public String isIsTop() {
                return isTop;
            }

            public void setIsTop(String isTop) {
                this.isTop = isTop;
            }

            public String getScore() {
                return score;
            }

            public void setScore(String score) {
                this.score = score;
            }

            public String getPrizeId() {
                return prizeId;
            }

            public void setPrizeId(String prizeId) {
                this.prizeId = prizeId;
            }

            public String getPrizeOrderId() {
                return prizeOrderId;
            }

            public void setPrizeOrderId(String prizeOrderId) {
                this.prizeOrderId = prizeOrderId;
            }

            public String getOnShelve() {
                return onShelve;
            }

            public void setOnShelve(String onShelve) {
                this.onShelve = onShelve;
            }

            public ReplyLv2Model getReply() {
                return reply;
            }

            public void setReply(ReplyLv2Model reply) {
                this.reply = reply;
            }

            public String getRnikeName() {
                return rnikeName;
            }

            public void setRnikeName(String rnikeName) {
                this.rnikeName = rnikeName;
            }

            public String getRcommentId() {
                return rcommentId;
            }

            public void setRcommentId(String rcommentId) {
                this.rcommentId = rcommentId;
            }

            public String getPcommentId() {
                return pcommentId;
            }

            public void setPcommentId(String pcommentId) {
                this.pcommentId = pcommentId;
            }

            public String getRuserId() {
                return ruserId;
            }

            public void setRuserId(String ruserId) {
                this.ruserId = ruserId;
            }

            public String isOfficial() {
                return official;
            }

            public void setOfficial(String official) {
                this.official = official;
            }

            public String getOfficial() {
                return official;
            }

            public int getPosition() {
                return position;
            }

            public void setPosition(int position) {
                this.position = position;
            }

            public boolean isShow() {
                return isShow;
            }

            public void setShow(boolean show) {
                isShow = show;
            }


            public List<ReplyLv2Model.ReplyListDTO> getReplyLv2CacheList() {
                return replyLv2CacheList;
            }

            public void setReplyLv2CacheList(List<ReplyLv2Model.ReplyListDTO> replyLv2CacheList) {
                this.replyLv2CacheList = replyLv2CacheList;
            }

            public List<ReplyLv2Model.ReplyListDTO> getReplyLv2Alllist() {
                return replyLv2Alllist;
            }

            public void setReplyLv2Alllist(List<ReplyLv2Model.ReplyListDTO> replyLv2Alllist) {
                this.replyLv2Alllist = replyLv2Alllist;
            }
        }
    }
}
