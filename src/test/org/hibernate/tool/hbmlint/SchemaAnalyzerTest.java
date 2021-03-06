package org.hibernate.tool.hbmlint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Table;
import org.hibernate.tool.JDBCMetaDataBinderTestCase;
import org.hibernate.tool.hbm2x.HbmLintExporter;
import org.hibernate.tool.hbmlint.detector.SchemaByMetaDataDetector;
import org.hibernate.tool.util.MetadataHelper;

public class SchemaAnalyzerTest extends JDBCMetaDataBinderTestCase {

	public SchemaAnalyzerTest() {
		super();
	}

	protected String[] getMappings() {
		return new String[] { "hbmlint/SchemaIssues.hbm.xml" };
	}

	static class MockCollector implements IssueCollector {
		List<Issue> problems = new ArrayList<Issue>();		
		public void reportIssue(Issue analyze) {			
			problems.add(analyze);
		}		
	}
	
	public void testSchemaAnalyzer() {
		Configuration configuration = new Configuration();
		addMappings( getMappings(), configuration );
		SchemaByMetaDataDetector analyzer = new SchemaByMetaDataDetector();
		analyzer.initialize( MetadataHelper.getMetadata(configuration) );
		
		Metadata metadata = MetadataHelper.getMetadata(configuration);
		
		Iterator<Table> tableMappings = metadata.collectTableMappings().iterator();
		
		while ( tableMappings.hasNext() ) {
			Table table = tableMappings.next();
		
			MockCollector mc = new MockCollector();
			
			if(table.getName().equalsIgnoreCase( "missingtable" )) {
				analyzer.visit(table, mc );				
				assertEquals(mc.problems.size(),1);
				Issue ap = (Issue) mc.problems.get( 0 );
				assertTrue(ap.getDescription().indexOf( "Missing table" ) >=0);
			} else if(table.getName().equalsIgnoreCase( "category" )) {
				analyzer.visit(table, mc );
				assertEquals(mc.problems.size(),1);
				Issue ap = (Issue) mc.problems.get( 0 );
				assertTrue(ap.getDescription().indexOf( "missing column: name" ) >=0);							
			} else if(table.getName().equalsIgnoreCase( "badtype" )) {
				analyzer.visit(table, mc );
				assertEquals(mc.problems.size(),1);
				Issue ap = (Issue) mc.problems.get( 0 );
				assertTrue(ap.getDescription().indexOf( "wrong column type for name" ) >=0);
			}
		}
		
		MockCollector mc = new MockCollector();
		analyzer.visitGenerators(mc);
		assertEquals(1,mc.problems.size());
		Issue issue = (Issue) mc.problems.get( 0 );
		assertTrue(issue.getDescription().indexOf( "does_not_exist" ) >=0);		
	}
			
	public void testExporter() {		
		Configuration configuration = new Configuration();
		new HbmLintExporter(configuration, getOutputDir()).start();			
	}
		
	protected String[] getCreateSQL() {
		return new String[] { "create table Category (id int, parent_id numeric(5))",
				"create table BadType (id int, name varchar(100))",
				"create sequence should_be_there start with 1",
				"create table hilo_table (id int)"};
	}

	protected String[] getDropSQL() {
		return new String[] { "drop table Category", "drop table BadType", "drop sequence should_be_there", "drop table hilo_table" };
	}
	
}
