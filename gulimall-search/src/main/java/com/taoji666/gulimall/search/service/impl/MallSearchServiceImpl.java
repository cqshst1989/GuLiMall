package com.taoji666.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.to.es.SkuEsModel;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.search.config.GuliMallElasticSearchConfig;
import com.taoji666.gulimall.search.constant.EsConstant;
import com.taoji666.gulimall.search.feign.ProductFeignService;
import com.taoji666.gulimall.search.service.MallSearchService;
import com.taoji666.gulimall.search.vo.AttrResponseVo;
import com.taoji666.gulimall.search.vo.BrandVo;
import com.taoji666.gulimall.search.vo.SearchParam;
import com.taoji666.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient esRestClient;

    @Resource
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) { //param是前端页面穿传来的 查询参数（筛选条件）
        // 动态构建出查询需要的DSL语句
        SearchResult result = null;

        //1、准备检索请求,所有的检索条件就设置在这个searchRequest里面，通过buildSearchRequest方法（自己写）
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2、执行检索请求   SearchResponse是es框架中的类
            SearchResponse response = esRestClient.search(searchRequest, GuliMallElasticSearchConfig.COMMON_OPTIONS);

            //3、构建结果数据，根据检索请求，和检索参数。构建方法，也是自己写
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //最终返回查询结果
        return result;
    }

    /**
     *将从ES中查好的数据封装进 SearchResult,返回前端
     * @Param ES中封装好的数据 SearchResponse response
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult(); //创建空参SearchResult，慢慢往里面Set，Set完成以后，返回给前端


        //1、返回的所有查询到的商品，  商品可以直接从hits（命中的记录）中查到
        SearchHits hits = response.getHits();

        List<SkuEsModel> esModels = new ArrayList<>(); //当时是SkuEsModel整个存进es，现在也是从es整个取出
        //遍历所有商品信息    参考dslResponse.json中的结构
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                   //商品信息都是放在hits 下面的hits 下面的 _source里面，原来是SkuEsModel整个存进去的，现在整个拿出来，爽
                String sourceAsString = hit.getSourceAsString(); //取出来的是JSON
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);//用JSON工具，转成SkuEsModel对象

                //判断是否按关键字检索，若是就显示高亮，否则不显示
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    //拿到高亮信息显示标题  高亮就是后端帮你写好html的高亮标签 b + style
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String skuTitleValue = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(skuTitleValue);
                }
                esModels.add(esModel); //将查到的esModel放进集合
            }
        }
        result.setProduct(esModels); //从集合中取出Product数据，放进SearchResult

        //2、当前商品涉及到的所有属性信息  有内部嵌套，多级子聚合
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        //获取属性信息的聚合
        ParsedNested attrsAgg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {

            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //2.1、得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();

            attrVo.setAttrId(attrId);

            //2.2、得到属性的名字  解开子子聚合
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();

            attrVo.setAttrName(attrName);

            //2.3、得到属性的所有值  属性值可以有多个，因此是集合List<String>
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");  //传入MultiBucketsAggregation.Bucket 参数，来执行getKeyAsString并返回其结果
            List<String> attrValues = attrValueAgg.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVo.setAttrValue(attrValues);
               /* 匿名表达式二级简写 :: ，将attrValueAgg.getBuckets() 传来的MultiBucketsAggregation.Bucket buckets匿名了，下面是item代表buckets
               map(item -> {
               String KeyAsString = ((Terms.Bucket) item).getKeyAsString();
               * return keyAsString;})*/

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos); //传入SearchResult

        //2.3、当前商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        //获取到品牌的聚合  先用response得到brandAgg
        ParsedLongTerms brandAgg = response.getAggregations().get("brand_agg");
        //从聚合的buckets，以及子聚合 子buckets中获取数据进行封装
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();

            //2.3.1、得到品牌的id  id就在第一次聚合中，很容易拿到
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);

            //2.3.2、得到品牌的名字  Name在子聚合中，需要再进去一次
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            //2.3.3、得到品牌的图片  Img也在子聚合中，需要再进去一次
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //4、当前商品涉及到的所有分类信息
        //获取到分类的聚合(buckets) 响应聚合查询时的聚合 agg，最外层聚合是brandAgg,catalogAgg,attrs
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = response.getAggregations().get("catalog_agg");
        //buckets 涉及到的具体分类信息，是一个集合。即catalogAgg.getBuckets()是一个集合
        //循环取出集合中的brandId 和 brandName
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) { //取出最外层buckets （外层agg聚合）
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString(); //取出嵌套buckets key（子聚合bagg）
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            //得到分类名
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }

        result.setCatalogs(catalogVos);
        //===============以上可以从聚合信息中获取====================//

        //5、分页信息-页码，页码直接来源于请求参数，直接设置
        result.setPageNum(param.getPageNum());

        //5、1分页信息、总记录数   总记录数，就是hits下total的value
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        //5、2分页信息-总页码-自行计算了
        /*
        * 总记录数 11条  11/2 =5....1  有余数，说明页码是5+1页，没有余数，页码就刚好是5
        * */
        int totalPages = (int) total % EsConstant.PRODUCT_PAGE_SIZE == 0 ? //total是long类型因此需要转换
                (int) total / EsConstant.PRODUCT_PAGE_SIZE : ((int) total / EsConstant.PRODUCT_PAGE_SIZE + 1);
       //计算出页码后，就set 进 result
        result.setTotalPages(totalPages);

        //封装商城的所有页码 右下角的第1页 2页 3页.....直到第 totalPages页
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs); //从1 开始 直到totalPages

        //6、构建面包屑导航  result.setPageNavs()

        /* 面包屑导航数据   就是已经选定的  筛选条件(属性) 展示，并且点击X可以删除筛选条件，
         * 比如： 操作系统：IOS X   品牌：苹果 X
         *
         * 一旦选中了一个属性（筛选条件），这个属性就会变成面包屑。同时会根据该面包屑，再生成新的商品属性，和商品数据
         * 一旦点击X 去掉某个面包屑，也会导致页面的商品属性和商品数据变化
         *
         * 具体上淘宝试试就知道了
         * */
        //param 就是请求参数  SearchParam
        if (param.getAttrs() != null && param.getAttrs().size() > 0) { //判定！=null，如果请求参数里面有属性，都是防止空指针异常
        //通过商品属性attrs，构建出NavVo
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                //6.1、分析每一个attrs传过来的参数值
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //attrs=2_5寸:6寸   属性2是 5寸或者6寸
                String[] s = attr.split("_"); //分割属性 和 属性值
                navVo.setNavValue(s[1]); //属性值数组[5寸,6寸]
                R r = productFeignService.attrInfo(Long.parseLong(s[0])); //取出属性attrId对应的所有属性

                if (r.getCode() == 0) { //说明成功，正常返回
                    //getData是自己写的转换方法，前面远程调用attrInfo，给R存入的是attr 即 return R.ok().put("attr", respVo)
                     //值为AttrRespVo respVo。  为了方便，自己在本微服务写一个AttrResponseVo，照抄AttrRespVo
                    //将远程的AttrRespVo类型 由于R通病，转成了Object，再快速 转换为 AttrResponseVo 类型。。（两个类属性就一模一样）
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else { //如果远程调用失败，就用attrId做名字
                    navVo.setNavName(s[0]);
                }

                //6.2、取消了这个面包屑以后，即这个面包屑 对应的查询条件 以后。显示的商品属性，和商品内容
                //思路：查询条件 URL上都有。取消查询条件，自然取消了URL上的某些筛选字段
                //我们要做的就是，拿到URL中所有的查询条件，去掉当前X掉的查询条件

                //编码，attr里面有中文attrs=2_5寸:6寸，不做编码UTF-8 就乱码
                String replace = replaceQueryString(param,attr,"attrs");

                //设置好，要跳转的URL
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(collect); //将面包屑信息，不管是X掉的，还是新增的 加入SearchResult
        }
        //7、仿照属性，品牌也要面包屑导航功能。  具体功能是前端来做，改个超链接，微服务做的就是给searchResult的属性赋值
        /*
        * 筛选条件中，选择品牌后。  筛选的品牌消失，面包屑出现品牌。
        * 删除品牌后。面包屑那里品牌消失， 筛选那里重新出现品牌
        * 当然伴随商品数据的变换
        * */

        //param 就是请求参数  SearchParam  如果请求参数中有品牌ID，就是说客户选中了按品牌筛选 即：品牌ID从筛选条件栏目 转入 面包屑导航。 商品数据展示选定品牌的商品
        if (param.getBrandId()!=null && param.getBrandId().size()>0){
            List<SearchResult.NavVo> navs = result.getNavs(); //先拿到，根据前端请求URL 从ES中获取到的查询结果result，中的面包屑数据，

            SearchResult.NavVo navVo = new SearchResult.NavVo(); //要添加进面包屑

            navVo.setNavName("品牌");

            //远程 查询所有品牌
            R r = productFeignService.brandsInfo(param.getBrandId());

            if (r.getCode()==0){
                List<BrandVo> brand = r.getData("brand",new TypeReference<List<BrandVo>>(){});
                StringBuffer buffer = new StringBuffer(); //就是可变容量的String，显然getBrandName 一直在变
                String replace = "";
                for (BrandVo brandVo : brand){
                    buffer.append(brandVo.getBrandName()+";");
                    replace = replaceQueryString(param,brandVo.getBrandId()+"","brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
            }
            navs.add(navVo);

        }

        return result;
    }

    private  String replaceQueryString(SearchParam param,String value,String key) {
        String encode = null;
        try {
            /*
             * 如果面包屑已经有两个属性被选中 cpu品牌：海思 X  cpu型号：麒麟 E9700 X  （9700前面有空格）
             * 浏览器会处理成...&attrs=15_海思&attrs16_麒麟%20E9700
             * java UTF-8处理空格，会把空格 变成 +
             * */

            //将attr编码成UTF-8 赋值给encode，但是编码后的attr，里面的空格，都变成+了
            encode = URLEncoder.encode(value, "UTF-8");

            //为了让浏览器能够识别，将编码后的 +  都变成 %20
            encode.replace("+", "%20");  //浏览器对空格的编码和Java不一样，差异化处理
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //用空串 替换掉 原来的查询条件
        //将attrs=15_海思 改成空
        return param.get_queryString().replace("&"+key + encode, "");
    }

    /**
     * 构建检索请求
     * 检索请求会涉及：模糊匹配，过滤（按照属性、分类、品牌，价格区间，库存），完成排序、分页、高亮,聚合分析功能
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        // 检索请求构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * 构建DSL语句：核心要点是  按照dsl.json对应ES的 层次结构
         * 查询：模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存）
         */
        //1. 构建 bool（复合查询）： 复合语句可以合并，任何其他查询语句，包括复合语句。这也就意味着，复合语句之间可以互相嵌套，可以表达非常复杂的逻辑。
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //1.1 bool-must 模糊匹配must:必须达到must所列举的所有条件  参考dsl.json
        if (!StringUtils.isEmpty(param.getKeyword())) { //如果前端请求中有keyword
            //将前端请求的keyword 比如华为 （就从skuTitle中找华为）
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        //1.2bool-filter：对结果进行过滤，但不计算相关性得分。

        //1.2.1 bool-filter : catalog  如果前端请求带了三级分类catalogId，就按照三级分类查
        if (null != param.getCatalog3Id()) {     //非文本字段，使用term来精确检索  ps:文本用match精确检索
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        //1.2.2 bool-filter : brandId 按照品牌id查询
        // 如果页面传来的品牌ID有值（可以是多个值），因此使用terms（精确匹配多个id），比如前端请求是1、2、9
        if (null != param.getBrandId() && param.getBrandId().size() > 0) {    //param.getBrandId() 是多个id比如，1、2、9
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        //1.2.3 bool-filter attrs 按照指定的属性查询
        //如果前端请求中传来了属性
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            param.getAttrs().forEach(item -> {
                //前端请求....attrs=1_5寸:8寸&attrs=2_16G:8G
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

                //attrs=1_5寸:8寸
                String[] s = item.split("_");
                String attrId = s[0]; // 检索的属性id 即 1 （_的左边第一个就是1）
                String[] attrValues = s[1].split(":");//_的右边S[1] 即5寸:8寸，也就是属性值 用：继续分割成数组attrValues=[5寸,8寸]

                boolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));

                // 每一个属性都要生成一个 nested 查询  ScoreMode.None不参与评分
                //本句才是精华，前面的都是本句的入参
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", boolQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder); //对准dsl.json看层次关系
            });

        }
        //1.2.4 bool-filter hasStock 按照是否有库存查询
        //页面请求会传来0（无库存） 或 1（有库存）因此依然使用term。注意：es中是布尔，但是请求是0和1，代码已经巧妙转换
        if (null != param.getHasStock()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        //1.2.5 skuPrice bool-filter 按照价格区间查询（有三种情况）
        //如果前端请求中有价格区间，区间对应range
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
           /*
           skuPrice形式为：1_500或_500或500_
            "range": { //区间查询
                "skuPrice": {  //比如页面请求查询1000 到 7000 的商品
                    "gte": 1000,
                            "lte": 7000
                }
             */
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            //将请求中的1000_7000 用“_”进行分割，会得到一个数组
            String[] price = param.getSkuPrice().split("_");
            if (price.length == 2) { //如果数组为2  [1000,7000] 自然就是区间
                rangeQueryBuilder.gte(price[0]).lte(price[1]);//gte是大于，lte是小于。大于第一个数，小于第二个数
            } else if (price.length == 1) { //等于1，就得 继续判断 是 大于某个值 还是 小于某个值
                if (param.getSkuPrice().startsWith("_")) { //_500
                    rangeQueryBuilder.lte(price[1]);
                }
                if (param.getSkuPrice().endsWith("_")) { //500_
                    rangeQueryBuilder.gte(price[0]);
                }
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        // 封装所有的查询条件 (最外层的查询) 参考dsl.json看层次结构  query过了就是bool
        searchSourceBuilder.query(boolQueryBuilder);


        /**
         * 排序，分页，高亮   dsl.json中，和bool平级，但在bool后面，查出结果后排序
         */
        // 2.1 排序  形式为sort=hotScore_asc/desc

        if (!StringUtils.isEmpty(param.getSort())) { //依然先判断页面请求是否带排序字段
            String sort = param.getSort();
            // sort=hotScore_asc/desc
            String[] sortFields = sort.split("_"); //_左边是排序字段sortFields[0]  右边是升序或降序

            SortOrder sortOrder = sortFields[1].equalsIgnoreCase(sortFields[1]) ? SortOrder.ASC : SortOrder.DESC;
            searchSourceBuilder.sort(sortFields[0], sortOrder);
        }

        // 2.2 分页 from = (pageNum - 1) * pageSize   ps:pageNum在SearchParam类中 默认是1  ES中索引从0开始，因此-1
        //每页2个数据，从1页（es中0）开始就是从第0个数据开始，从第二页开始从（2-1）*2个数据开始
        searchSourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGE_SIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGE_SIZE);

        // 2.3 高亮  有keyword才高亮，如果没有keyword 高亮也没意义
        //判定有没有keyword
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();

            //类似写html<b style='color:red'>前面查到的skuTitle的值</b>
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            searchSourceBuilder.highlighter(highlightBuilder);
        }

        System.out.println("构建的DSL语句" + searchSourceBuilder.toString()); //测试下前面构建的DSL是不是和dsl.json一模一样

        /**
         * 聚合分析  由于就是从查到的SkuEsModel中，聚合该类中的关注属性  一定要看dsl.json的层次结构
         * brandAgg、catalogAgg、attrAgg是第一级聚合 （分别对应SkuEsModel中的三张数据表的属性）
         */
        //1. 按照品牌进行聚合（SkuEsModel中的pms_brand数据表字段的三个属性）      给这个聚合取名为"brand_agg"
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        //1.1 品牌brandAgg的子聚合-品牌名聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        //1.2 品牌brandAgg的子聚合-品牌图片聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));

        //配置完成后，构建聚合
        searchSourceBuilder.aggregation(brand_agg);

        //2. 按照分类信息进行聚合 对应SkuEsModel中pms_category数据表字段的2个属性
        //2.1 先new一个聚合，取好名字
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        //2.2 设置聚合条件，
        catalog_agg.field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        // 3. 按照属性信息进行聚合 SkuEsModel属性中pms_attr数据表对应的 内部类 属性，由于内部类，二次子聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");

        //3.1 按照属性ID进行一次聚合
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attr_agg.subAggregation(attr_id_agg);

        //3.1.1 在每个属性ID下，按照属性名进行二次聚合
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));

       /* 3.1.2 在每个属性ID下，按照属性值进行聚合  一个属性名可以对应多个属性值，比如上市年份，可以对应多个值。
        mysql中就是多行同名 attrName（上市年份） 对应不同 attrValue（2020、2021）*/
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        //最后，将配置好的聚合条件的attr_agg，放到最大的构造器里面去聚合，完成聚合
        searchSourceBuilder.aggregation(attr_agg);

        //测试，打印出的DSL对不对
        String s = searchSourceBuilder.toString();
        System.out.println("构建的DSL语句"+s);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }
}
