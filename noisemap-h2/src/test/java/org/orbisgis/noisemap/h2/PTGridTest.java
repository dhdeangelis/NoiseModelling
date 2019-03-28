package org.orbisgis.noisemap.h2;

import org.h2.util.StringUtils;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.orbisgis.noisemap.h2.BR_EvalSourceC.evalSourceC;

/**
 * @author Nicolas Fortin
 */
public class PTGridTest {
    private static Connection connection;
    private Statement st;

    @BeforeClass
    public static void tearUpClass() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PTGridTest.class.getSimpleName() + UUID.randomUUID(), false, "MV_STORE=FALSE"));
        org.h2gis.functions.factory.H2GISFunctions.load(connection);
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid3D(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new CNOSSOS_Att(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_SpectrumRepartition(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_EvalSource(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_SpectrumRepartition(), "");
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        connection.close();
    }

    @Before
    public void setUp() throws Exception {
        st = connection.createStatement();
    }

    @After
    public void tearDown() throws Exception {
        st.close();
    }


    private static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(PTGridTest.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+ StringUtils.quoteStringSQL(resourceFile.getPath());
    }

    @Test
    public void testForAntoine(){
        evalSourceC(14.0, 0.0, 22.0, 0.0, 0.0, 7, 0, 0, 0, 0, 0.0, 0.0, 55.96996083076022, "FR_R2", 15.0, 0.0, 0.0, 200.0, 1, 63);
    }

    @Test
    public void testFreeField() throws SQLException {
        // Create empty buildings table
        st.execute("DROP TABLE IF EXISTS BUILDINGS");
        st.execute("CREATE TABLE BUILDINGS(the_geom POLYGON)");
        // Create a single sound source
        st.execute("DROP TABLE IF EXISTS roads_src_global");
        st.execute("CREATE TEMPORARY TABLE roads_src_global(the_geom POINT, db_m double)");
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(0 0)'::geometry, 85)");
        // INSERT 2 points to set the computation area
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(-20 -20)'::geometry, 0)");
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(20 20)'::geometry, 0)");
        // Compute spectrum repartition
        st.execute("drop table if exists roads_src;\n" +
                "CREATE TABLE roads_src AS SELECT the_geom,\n" +
                "BR_SpectrumRepartition(100,1,db_m) as db_m100,\n" +
                "BR_SpectrumRepartition(125,1,db_m) as db_m125,\n" +
                "BR_SpectrumRepartition(160,1,db_m) as db_m160,\n" +
                "BR_SpectrumRepartition(200,1,db_m) as db_m200,\n" +
                "BR_SpectrumRepartition(250,1,db_m) as db_m250,\n" +
                "BR_SpectrumRepartition(315,1,db_m) as db_m315,\n" +
                "BR_SpectrumRepartition(400,1,db_m) as db_m400,\n" +
                "BR_SpectrumRepartition(500,1,db_m) as db_m500,\n" +
                "BR_SpectrumRepartition(630,1,db_m) as db_m630,\n" +
                "BR_SpectrumRepartition(800,1,db_m) as db_m800,\n" +
                "BR_SpectrumRepartition(1000,1,db_m) as db_m1000,\n" +
                "BR_SpectrumRepartition(1250,1,db_m) as db_m1250,\n" +
                "BR_SpectrumRepartition(1600,1,db_m) as db_m1600,\n" +
                "BR_SpectrumRepartition(2000,1,db_m) as db_m2000,\n" +
                "BR_SpectrumRepartition(2500,1,db_m) as db_m2500,\n" +
                "BR_SpectrumRepartition(3150,1,db_m) as db_m3150,\n" +
                "BR_SpectrumRepartition(4000,1,db_m) as db_m4000,\n" +
                "BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from roads_src_global;");
        // Create receivers points
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(5 0)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(10 0)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(15 0)')");
        // Compute noise map
        st.execute("DROP TABLE IF EXISTS TEST");
        ResultSet rs = st.executeQuery("SELECT * FROM BR_PTGRID('buildings', 'roads_src','receivers', '', 50,50, 2,2,0.2)");
        try {
            assertTrue(rs.next());
            assertEquals(1l, rs.getLong("GID"));
            assertEquals(0, rs.getInt("CELL_ID"));
            assertEquals(59.89, 10*Math.log10(rs.getDouble("W")), 0.01);
            assertTrue(rs.next());
            assertEquals(2l, rs.getLong("GID"));
            assertEquals(0, rs.getInt("CELL_ID"));
            assertEquals(53.84, 10*Math.log10(rs.getDouble("W")), 0.01);
            assertTrue(rs.next());
            assertEquals(3l, rs.getLong("GID"));
            assertEquals(0, rs.getInt("CELL_ID"));
            assertEquals(50.3, 10*Math.log10(rs.getDouble("W")), 0.01);
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    @Test
    public void testCnossosAtt() throws SQLException {
        // Create empty buildings table
        st.execute("DROP TABLE IF EXISTS BUILDINGS");
        st.execute("CREATE TABLE BUILDINGS(the_geom POLYGON)");
        // Create a single sound source
        st.execute("DROP TABLE IF EXISTS roads_src_global");
        st.execute("CREATE TEMPORARY TABLE roads_src_global(id integer,the_geom POINT, db_m double)");
        // INSERT 2 points to set the computation area
        st.execute("INSERT INTO roads_src_global VALUES (1,'POINT(200 50 4)'::geometry, 100)");
        st.execute("INSERT INTO roads_src_global VALUES (2,'POINT(-180 -30 4)'::geometry, 80)");
        //st.execute("INSERT INTO roads_src_global VALUES (2,'POINT(10 10 5)'::geometry, 90)");


        // Compute spectrum repartition
        st.execute("drop table if exists roads_src;\n" +
                "CREATE TABLE roads_src AS SELECT id, the_geom,\n" +
                "db_m as db_m63,\n" +
                "db_m as db_m125,\n" +
                "db_m as db_m250,\n" +
                "db_m as db_m500,\n" +
                "db_m as db_m1000,\n" +
                "db_m as db_m2000,\n" +
                "db_m as db_m4000,\n" +
                "db_m as db_m8000 from roads_src_global;");
        // Create receivers points
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(10 10 1)')");
       // st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(50 50 1)')");
       // st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(-50 -50 1)')");
        // Compute noise map
        st.execute("DROP TABLE IF EXISTS TEST");

        ResultSet rs = st.executeQuery("SELECT * FROM CNOSSOS_ATT('buildings','', 'roads_src','receivers','DB_M','', '', 250,250, 2,1,0.2)");
        try {
            assertTrue(rs.next());
            assertEquals(1, rs.getLong("IDSOURCE"));
            assertEquals(1, rs.getInt("IDRECEPTEUR"));
            assertEquals(-53.05, rs.getDouble("ATT"), 1);
            assertTrue(rs.next());
            assertEquals(2, rs.getLong("IDSOURCE"));
            assertEquals(1, rs.getInt("IDRECEPTEUR"));
            assertEquals(-53.05, rs.getDouble("ATT"), 1);
        } finally {
            rs.close();
        }
    }


    @Test
    public void testDem() throws Exception {
        st.execute("drop table if exists sound_source;\n" +
                "create table sound_source(the_geom geometry, db_m100 double,db_m125 double,db_m160 double," +
                "db_m200 double,db_m250 double,db_m315 double,db_m400 double,db_m500 double,db_m630 double,\n" +
                "db_m800 double,db_m1000 double,db_m1250 double,db_m1600 double,db_m2000 double,db_m2500 double," +
                "db_m3150 double,db_m4000 double,db_m5000 double);\n" +
                "insert into sound_source VALUES ('POINT(55 60 1)', 100, 100, 100, 100, 100, 100, 100, 100, 100, 100," +
                " 100, 100, 100, 100, 100, 100, 100, 100);\n" +
                "INSERT INTO sound_source VALUES ('POINT( -300 -300 0 )',Log10(0),Log10(0),Log10(0),Log10(0)," +
                "Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0)," +
                "Log10(0),Log10(0),Log10(0));\n" +
                "INSERT INTO sound_source VALUES ('POINT( 500 500 0 )',Log10(0),Log10(0),Log10(0),Log10(0),Log10(0)," +
                "Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0)," +
                "Log10(0),Log10(0));" +
                "");
        st.execute(getRunScriptRes("dem.sql"));
        st.execute("drop table if exists pt_lvl");
        // Create receivers points
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-250.56607923708552 106.76760851263573 1.6)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-250.56607923708552 106.76760851263573 25)')");
        ResultSet rs = st.executeQuery("select * from BR_PTGRID3D('BUILDINGS', 'SOUND_SOURCE','RECEIVERS', '','DEM' ,1000,500, 2, 1, 0.2)");
        //  W must be equal to 1
        try {
            assertTrue(rs.next());
            // First receiver is hidden by the hill
            assertEquals(1, rs.getDouble("W"), 0.01);
            // Second receiver is higher than the hill
            assertTrue(rs.next());
            assertEquals(52.5, 10 * Math.log10(rs.getDouble("W")), 0.1);
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    // TODO reanable test cnossos
    public void testDemTopBuilding() throws Exception {
        st.execute(getRunScriptRes("dem.sql"));
        st.execute("drop table if exists sound_source;\n" +
                "create table sound_source(the_geom geometry, db_m100 double,db_m125 double,db_m160 double," +
                "db_m200 double,db_m250 double,db_m315 double,db_m400 double,db_m500 double,db_m630 double,\n" +
                "db_m800 double,db_m1000 double,db_m1250 double,db_m1600 double,db_m2000 double,db_m2500 double," +
                "db_m3150 double,db_m4000 double,db_m5000 double);\n" +
                "insert into sound_source VALUES ('POINT(68 60 5.5)', 100, 100, 100, 100, 100, 100, 100, 100, 100, 100," +
                " 100, 100, 100, 100, 100, 100, 100, 100);");
        st.execute("drop table if exists pt_lvl");
        // Create receivers points
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-157 60 0.438205)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-157 60 -0.361795)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-157 60 11.038205)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (59 60 1.6)')");
        ResultSet rs = st.executeQuery("select * from BR_PTGRID3D('BUILDINGS', 'SOUND_SOURCE','RECEIVERS', '','DEM' ,1000,500, 2, 1, 0.2)");
        //  W must be equal to 1
        try {
            assertTrue(rs.next());
            // First receiver is hidden by the hill
            assertEquals(1, rs.getDouble("W"), 0.01);
            assertTrue(rs.next());
            // Second receiver is under the ground
            assertEquals(1, rs.getDouble("W"), 0.01);
            // Third receiver is higher than the hill
            assertTrue(rs.next());
            assertEquals(28.24, 10 * Math.log10(rs.getDouble("W")), 0.1);
            // Fourth receiver is just behind the wall of building. Only horizontal diffraction on edge is contributing
            assertTrue(rs.next());
            assertEquals(65.63, 10 * Math.log10(rs.getDouble("W")), 0.1);
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    // TODO reanable test cnossos
    public void testGroundReflectionPropagation() throws Exception {
        st.execute("DROP TABLE IF EXISTS LANDCOVER2000");
        st.execute("CALL SHPREAD('"+TriGridTest.class.getResource("landcover2000.shp").getFile()+"', 'LANDCOVER2000')");
        st.execute(getRunScriptRes("ground-effect.sql"));
        ResultSet rs = st.executeQuery("SELECT * FROM PT_LVL");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("GID"));
        assertEquals(56.91,10*Math.log10(rs.getDouble("W")), 0.1);
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("GID"));
        assertEquals(55.21,10*Math.log10(rs.getDouble("W")), 0.1);
        assertFalse(rs.next());
    }



    @Test
    public void testAlphaColumn() throws SQLException {
        // Create empty buildings table
        st.execute("DROP TABLE IF EXISTS BUILDINGS");
        st.execute("CREATE TABLE BUILDINGS(the_geom POLYGON, HEIGHT double, ALPHA double)");
        st.execute("INSERT INTO BUILDINGS VALUES ('POLYGON ((12.2 14.6, 11.5 37.3, 72.8 37.3, 71.5 12.9, 12.2 14.6))', 4, 0.28)");
        st.execute("INSERT INTO BUILDINGS VALUES ('POLYGON ((84.4 -29.8, 87.8 -10.8, 179.3 -12.2, 173.9 -32.5, 84.4 -29.8))', 4, 0.12)");
        // Create a single sound source
        st.execute("DROP TABLE IF EXISTS roads_src_global");
        st.execute("CREATE TEMPORARY TABLE roads_src_global(the_geom POINT, db_m double)");
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(0 0)'::geometry, 85)");
        // INSERT 2 points to set the computation area
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(-100 -100)'::geometry, 0)");
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(100 100)'::geometry, 0)");
        // Compute spectrum repartition
        st.execute("drop table if exists roads_src;\n" +
                "CREATE TABLE roads_src AS SELECT the_geom,\n" +
                "BR_SpectrumRepartition(100,1,db_m) as db_m100,\n" +
                "BR_SpectrumRepartition(125,1,db_m) as db_m125,\n" +
                "BR_SpectrumRepartition(160,1,db_m) as db_m160,\n" +
                "BR_SpectrumRepartition(200,1,db_m) as db_m200,\n" +
                "BR_SpectrumRepartition(250,1,db_m) as db_m250,\n" +
                "BR_SpectrumRepartition(315,1,db_m) as db_m315,\n" +
                "BR_SpectrumRepartition(400,1,db_m) as db_m400,\n" +
                "BR_SpectrumRepartition(500,1,db_m) as db_m500,\n" +
                "BR_SpectrumRepartition(630,1,db_m) as db_m630,\n" +
                "BR_SpectrumRepartition(800,1,db_m) as db_m800,\n" +
                "BR_SpectrumRepartition(1000,1,db_m) as db_m1000,\n" +
                "BR_SpectrumRepartition(1250,1,db_m) as db_m1250,\n" +
                "BR_SpectrumRepartition(1600,1,db_m) as db_m1600,\n" +
                "BR_SpectrumRepartition(2000,1,db_m) as db_m2000,\n" +
                "BR_SpectrumRepartition(2500,1,db_m) as db_m2500,\n" +
                "BR_SpectrumRepartition(3150,1,db_m) as db_m3150,\n" +
                "BR_SpectrumRepartition(4000,1,db_m) as db_m4000,\n" +
                "BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from roads_src_global;");
        // Create receivers points
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(167.7 8.13)')");
        // Compute noise map
        st.execute("DROP TABLE IF EXISTS TEST");
        ResultSet rs = st.executeQuery("SELECT * FROM BR_PTGRID('buildings', 'roads_src','receivers','', 400,400, 2,0,0.2)");
        try {
            assertTrue(rs.next());
            assertEquals(1l, rs.getLong("GID"));
            assertEquals(0, rs.getInt("CELL_ID"));
            assertEquals(30.5, 10*Math.log10(rs.getDouble("W")), 0.1);
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }
}
