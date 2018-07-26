
package com.lucenetest.index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexDocs {

    static final Logger LOG = LoggerFactory.getLogger(IndexDocs.class);

    public static final FieldType TYPE_NOT_STORED = new FieldType();

    /** Indexed, not tokenized, omits norms, indexes
     *  DOCS_ONLY, stored */
    public static final FieldType TYPE_STORED = new FieldType();

    static {
        TYPE_NOT_STORED.setOmitNorms(true);
        TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOT_STORED.setTokenized(false);
        TYPE_NOT_STORED.freeze();

        TYPE_STORED.setOmitNorms(true);
        TYPE_STORED.setIndexOptions(IndexOptions.DOCS);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.freeze();
    }



    private IndexDocs() {}


    public static void main(String[] args) {

        String indexPath = "index-example-1";
        boolean create = true;

        Date start = new Date();
       try {
           LOG.info("Indexing to directory '" + indexPath + "'...");

           // index存放的目录
           Directory dir = FSDirectory.open(Paths.get(indexPath));

           // 所有的field设置同样的analyzer
           //Analyzer analyzer = new StandardAnalyzer();
           //IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

           // 为field设置自己的analyzer
           Map<String, Analyzer> fieldAnalyzerMap = new HashMap<>();
           fieldAnalyzerMap.put("title", new SimpleAnalyzer());
           fieldAnalyzerMap.put("content", new StandardAnalyzer());
           Analyzer defaultAnalyzer = new StandardAnalyzer();
           PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(defaultAnalyzer, fieldAnalyzerMap);
           IndexWriterConfig iwc = new IndexWriterConfig(wrapper);

           if (create) {
               // 创建一个新的index目录。如果index目录已经存在，则先清除旧的
               iwc.setOpenMode(OpenMode.CREATE);
           } else {
               // 如果index目录已经存在，则打开；否则，创建一个新的
               iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
           }

           // Optional: for better indexing performance, if you
           // are indexing many documents, increase the RAM
           // buffer.  But if you do this, increase the max heap
           // size to the JVM (eg add -Xmx512m or -Xmx1g):
           //
           // iwc.setRAMBufferSizeMB(256.0);

           IndexWriter writer = new IndexWriter(dir, iwc);


           Document doc = createDoc("abc", "hello world", 100.0D);
           writer.addDocument(doc);
           doc = createDoc("def", "hello wife", 102.3D);
           writer.addDocument(doc);

           writer.commit();

           doc = createDoc("def", "hello best", 300.4D);
           writer.addDocument(doc);

           // commit 的数据可以被搜索到，没有commit的数据搜索不到
           IndexReader indexReader = DirectoryReader.open(dir);
           IndexSearcher indexSearcher = new IndexSearcher(indexReader);

           //Term term = new Term("content", "hello");
           //Query query = new TermQuery(term);

           //Query query = DoublePoint.newExactQuery("price", 100.0D);
           Query query = DoublePoint.newRangeQuery("price", 100.0D, 300.0D);
           TopDocs topDocs = indexSearcher.search(query, 1);
           if (topDocs == null) {
               LOG.info("topDocs is null");
           } else {
               ScoreDoc[] hitDocs = topDocs.scoreDocs;
               LOG.info("totalHits: {}, topDocs size: {}", topDocs.totalHits, hitDocs.length);
               for (ScoreDoc scoreDoc : hitDocs) {
                   LOG.info("docID: {}, score: {}, shardIndex: {}", scoreDoc.doc, scoreDoc.score, scoreDoc.shardIndex);
                   Document d = indexSearcher.doc(scoreDoc.doc);
                   LOG.info("title: {}, content: {}, price: {}", d.get("title"), d.get("content"), d.get("price"));
               }

           }


            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here.  This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);

            // close()默认会执行commit()
            writer.close();

            Date end = new Date();
            LOG.info("time duration: {} milliseconds", end.getTime() - start.getTime());

        } catch (IOException e) {
           LOG.info(" caught a {}, message: {}", e.getClass(), e.getMessage());
        }


    }


    static Document createDoc(String title, String content, Double price) {

        Document doc = new Document();

        // 不存储doc值，搜索获取的docment获取不到titile field的值
        Field titleField = new StringField("title", title, Field.Store.NO);
        // Field titleField = new Field("title", title, TYPE_STORED);
        doc.add(titleField);

        // 存储doc值，搜索获取的docment可获取content field的值
        Field contentField = new TextField("content", content, Field.Store.YES);
        //Field contentField = new Field("content", content, TYPE_STORED);
        doc.add(contentField);

        // 非文本型的field, Point类型的Field不会存储field的值，所以需要再添加一个StoredField，才能获取该field的值
        Field priceField = new DoublePoint("price", price);
        doc.add(priceField);
        Field priceStoredField = new StoredField("price", price);
        doc.add(priceStoredField);

        // 测试结果： title: null, content: hello world

        return doc;

    }



}

