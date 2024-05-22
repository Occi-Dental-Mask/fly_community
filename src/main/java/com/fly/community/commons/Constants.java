package com.fly.community.commons;

/**
 * @description:
 * @author：occi
 * @date: 2024/5/18
*/
public class Constants {


    /**
     * 注册激活状态：0激活成功、1重复激活、2激活失败,在数据库中只存入-1，1两种状态（未激活，已激活）
     */
    public enum ActivationState {
        ACTIVATION_SUCCESS(0, "激活成功，您的账号可以正常使用了！"),
        ACTIVATION_REPEAT(1, "您的账号已经激活过了！"),
        ACTIVATION_FAILURE(2, "激活失败，激活码不正确！"),

        ACTIVATION_INITIAL(-1, "未激活状态");

        private String info;
        private Integer code;

        ActivationState(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        public Integer getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }
    }

    /**
     * 过期时间(长/短)
     */
    public static final class Time {
        public static final Integer LONG_EXPIRE_TIME = 3600 * 24 * 90;
        public static final Integer SHORT_EXPIRE_TIME = 3600 * 12;
    }


    /**
     * 评论类型（1：对帖子评论、2：对评论回复，即楼中楼）
     */
    public static final class CommentEntityType {
        public static final Integer COMMENT_TYPE = 1;
        public static final Integer REPLY_TYPE = 2;
    }

    public static final class EntityType {
        /**
         * 实体类型: 帖子
         */
        public static final int ENTITY_TYPE_POST = 1;

        /**
         * 实体类型: 评论
         */
        public static final int ENTITY_TYPE_COMMENT = 2;

        /**
         * 实体类型: 用户
         */
        public static final int ENTITY_TYPE_USER = 3;
    }



    /**
     * 奖品类型（1:文字描述、2:兑换码、3:优惠券、4:实物奖品）
     */
    public enum AwardType {
        /**
         * 文字描述
         */
        DESC(1, "文字描述"),
        /**
         * 兑换码
         */
        RedeemCodeGoods(2, "兑换码"),
        /**
         * 优惠券
         */
        CouponGoods(3, "优惠券"),
        /**
         * 实物奖品
         */
        PhysicalGoods(4, "实物奖品");

        private Integer code;
        private String info;

        AwardType(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }
    }

    /**
     * 发奖状态：0等待发奖、1发奖成功、2发奖失败
     */
    public enum AwardState {

        /**
         * 等待发奖
         */
        WAIT(0, "等待发奖"),

        /**
         * 发奖成功
         */
        SUCCESS(1, "发奖成功"),

        /**
         * 发奖失败
         */
        FAILURE(2, "发奖失败");

        private Integer code;
        private String info;

        AwardState(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }
    }


    public enum Ids {
        SnowFlake,
        ShortCode,
        RandomNumeric;
    }


    /**
     * redis缓存key
     */
    public static final class RedisKey {
        private static final String LOTTERY_ACTIVITY_STOCK_COUNT = "lottery_activity_stock_count_";
        public static String KEY_LOTTERY_ACTIVITY_STOCK_COUNT(Long activityId) {
            return LOTTERY_ACTIVITY_STOCK_COUNT + activityId;
        }

        // 抽奖活动库存锁 Key
        private static final String LOTTERY_ACTIVITY_STOCK_COUNT_TOKEN = "lottery_activity_stock_count_token_";

        public static String KEY_LOTTERY_ACTIVITY_STOCK_COUNT_TOKEN(Long activityId, Integer stockUsedCount) {
            return LOTTERY_ACTIVITY_STOCK_COUNT_TOKEN + activityId + "_" + stockUsedCount;
        }
    }



}
