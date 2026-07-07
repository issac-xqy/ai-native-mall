package org.example.java_ai.runner;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FAQ 知识库初始化器
 * 启动时检查 Redis 中是否已有 FAQ 向量，没有则批量构建
 */
@Slf4j
@Component
@Profile("!test")
public class FAQKnowledgeInitializer implements CommandLineRunner {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public FAQKnowledgeInitializer(
            @Qualifier("faqEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        log.info("开始初始化FAQ知识库...");
        try {
            if (hasFAQKnowledge()) {
                log.info("Redis中已存在FAQ知识库，跳过初始化");
                return;
            }
            List<TextSegment> faqs = buildFAQDatabase();
            log.info("共准备 {} 条FAQ知识，开始批量导入...", faqs.size());

            // 批量 embedding 并行入库
            int batchSize = 50;
            int total = faqs.size();
            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(i + batchSize, total);
                List<TextSegment> batch = faqs.subList(i, end);
                try {
                    batch.forEach(segment -> {
                        var emb = embeddingModel.embed(segment).content();
                        embeddingStore.add(emb, segment);
                    });
                    log.info("已导入 {}/{} 条FAQ", end, total);
                } catch (Exception e) {
                    log.error("FAQ批量导入失败: index={}-{}", i, end, e);
                }
            }
            log.info("FAQ知识库初始化完成，共 {} 条", total);
        } catch (Exception e) {
            log.error("FAQ知识库初始化失败", e);
        }
    }

    private boolean hasFAQKnowledge() {
        try {
            var testEmbedding = embeddingModel.embed("test").content();
            var results = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                    .queryEmbedding(testEmbedding)
                    .maxResults(1)
                    .minScore(0.0)
                    .build()
            );
            return results.matches() != null && !results.matches().isEmpty();
        } catch (Exception e) {
            log.warn("检查Redis FAQ知识库失败，将重新构建: {}", e.getMessage());
            return false;
        }
    }

    private List<TextSegment> buildFAQDatabase() {
        List<TextSegment> segments = new ArrayList<>();

        // 精选 FAQ，带实际可用的答案
        Object[][] faqs = {
            // 商品咨询
            {"商品咨询", "这款商品有什么特点？", "您可以在商品详情页查看完整的产品特点介绍，包括材质、功能、适用场景等信息。如有具体型号需求，请告知商品名称。"},
            {"商品咨询", "商品的质量怎么样？", "我们所有商品均经过严格的质量检测，支持七天无理由退换货。您也可以查看其他用户的真实评价作为参考。"},
            {"商品咨询", "这个产品的材质是什么？", "商品材质信息可在详情页的「规格参数」部分查看。如页面未详细说明，请联系在线客服获取具体材质信息。"},
            {"商品咨询", "商品有没有保修？", "大部分电子产品提供一年全国联保，具体保修政策请查看商品详情页或包装内的保修卡。"},
            {"商品咨询", "保修期是多久？", "不同品类保修期不同：手机/电脑一般为1年，大家电一般为1-3年。具体以商品页标注为准。"},
            {"商品咨询", "商品支持七天无理由退换吗？", "支持。只要商品保持完好（未使用、未拆封、不影响二次销售），签收后7天内可申请无理由退货。"},
            {"商品咨询", "这款有现货吗？", "商品页面会实时显示库存状态。显示「有货」即可正常下单，一般当天或次日发货。"},
            {"商品咨询", "什么时候能补货？", "补货时间因供应商和商品不同而异，建议点击「到货通知」按钮，到货后会第一时间通知您。"},
            {"商品咨询", "是正品吗？", "我们所有商品均为品牌官方授权正品，支持防伪查询。如有疑问可联系客服获取授权证明。"},
            {"商品咨询", "商品的尺寸是多少？", "详细尺寸信息在商品详情页的「规格参数」中标注。部分商品提供多尺码选择，请在购买时选择合适规格。"},
            {"商品咨询", "有哪些颜色可选？", "颜色选项在商品页面可以查看。点击颜色分类即可预览不同颜色的商品图片。"},
            {"商品咨询", "这款适合送人吗？", "大部分商品都适合送礼。我们提供礼品包装服务（部分商品支持），下单时可以选择礼品包装选项。"},
            {"商品咨询", "适合什么年龄段使用？", "商品适用年龄段在详情页有标注。如无明确标注，可参考用户评价中的使用反馈或咨询客服。"},
            {"商品咨询", "操作复杂吗？", "大部分商品附带详细使用说明书。部分商品还提供视频教程，可通过扫描包装二维码观看。"},

            // 价格优惠
            {"价格优惠", "现在是什么价格？", "商品页面显示的是实时价格。我们不定期推出促销活动，建议关注首页活动专区获取最新优惠信息。"},
            {"价格优惠", "还能便宜点吗？", "当前价格已是平台定价。您可以关注是否有可用的优惠券，或者等待大促活动享受更多优惠。"},
            {"价格优惠", "有优惠吗？", "您可以在「我的」-「优惠券」页面查看当前可用优惠券。新用户注册即送专属优惠券。"},
            {"价格优惠", "优惠券怎么领？", "进入「我的」页面，点击「优惠券中心」即可查看和领取当前可用的优惠券。"},
            {"价格优惠", "新用户有优惠吗？", "有的！新用户注册即享首单专属优惠，首次下单可享受立减优惠。"},
            {"价格优惠", "支持分期付款吗？", "目前支持花呗分期和信用卡分期。分期期数包括3期、6期、12期，部分银行支持免息分期。"},
            {"价格优惠", "买多有优惠吗？", "部分商品支持多件优惠，凑单满减等。具体优惠以商品页面显示的促销信息为准。"},
            {"价格优惠", "积分可以抵扣现金吗？", "可以。100积分=1元，下单时系统会自动提示是否使用积分抵扣。积分可通过购物、签到、评价等方式获取。"},

            // 物流配送
            {"物流配送", "什么时候发货？", "正常情况下，工作日下午4点前下单当天发货，4点后次日发货。周末及节假日订单顺延至下一个工作日。"},
            {"物流配送", "发什么快递？", "默认发顺丰速运（全国包邮商品）或中通快递。部分大件商品使用德邦物流配送。"},
            {"物流配送", "包邮吗？", "大部分商品满99元包邮。具体包邮条件请查看商品页面标注。"},
            {"物流配送", "多久能送到？", "顺丰一般1-3天送达，中通一般3-5天送达。具体送达时间以物流信息为准。"},
            {"物流配送", "怎么查物流？", "在「我的订单」中找到对应订单，点击「查看物流」即可实时追踪包裹状态。"},
            {"物流配送", "可以改收货地址吗？", "未发货的订单可以在订单详情中修改地址。已发货的订单请联系客服协助处理。"},
            {"物流配送", "能拒收吗？", "可以拒收。拒收后包裹会退回仓库，我们收到退货后会安排退款。"},

            // 售后服务
            {"售后服务", "退货流程是什么？", "在「我的订单」中找到对应订单，点击「申请退货」→ 选择退货原因 → 提交申请 → 审核通过后寄回商品 → 仓库收货后安排退款。"},
            {"售后服务", "退货多久能收到？", "仓库收到退货后1-3个工作日内完成质检并安排退款。退款到账时间取决于支付方式，一般1-7个工作日。"},
            {"售后服务", "退款退到哪里？", "退款将原路退回：余额支付的退回余额，微信支付的退回微信，支付宝支付的退回支付宝。"},
            {"售后服务", "质量有问题怎么办？", "签收后如发现质量问题，请在24小时内拍照联系客服。确认属于质量问题后可申请退换货，运费由我们承担。"},
            {"售后服务", "客服电话是多少？", "客服热线：400-XXX-XXXX，工作时间：周一至周日 9:00-21:00。您也可以直接在APP内联系在线客服。"},
            {"售后服务", "发票怎么开？", "下单时可以选择开具电子发票。如需补开发票，请在订单完成后30天内联系客服申请。"},
            {"售后服务", "维修怎么申请？", "在保修期内的商品，可在「我的订单」中申请售后维修。保修期外可联系客服获取付费维修服务。"},

            // 账户支付
            {"账户支付", "怎么注册账号？", "点击「注册」按钮，输入手机号获取验证码，设置密码后即可完成注册。也可以使用微信一键登录。"},
            {"账户支付", "忘记密码怎么办？", "在登录页面点击「忘记密码」，通过绑定的手机号接收验证码后即可重置密码。"},
            {"账户支付", "支付方式有哪些？", "支持微信支付、支付宝、银行卡支付、余额支付。部分商品还支持货到付款。"},
            {"账户支付", "余额怎么充值？", "进入「我的」-「我的钱包」，点击「充值」按钮，选择充值金额和支付方式即可。"},
            {"账户支付", "重复扣款怎么办？", "如发生重复扣款，系统会在1-3个工作日内自动退款。如未到账请联系客服核实处理。"},
        };

        for (Object[] faq : faqs) {
            String category = (String) faq[0];
            String question = (String) faq[1];
            String answer = (String) faq[2];
            String content = "【" + category + "】问：" + question + " 答：" + answer;
            var metadata = dev.langchain4j.data.document.Metadata.metadata("type", "faq")
                    .put("category", category);
            segments.add(TextSegment.from(content, metadata));
        }

        return segments;
    }
}
