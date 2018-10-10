package algorithms;

import db.DBconnection;
import literals.Literals;
import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.SparseMatrix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * PageRank.java - a java class that computes the page rank scores of the crawled documents.
 * @author  Vishnu Narayanan Surendran
 * @version 1.0
 */

public class PageRank {
   List<Integer> docids = new ArrayList<Integer>();
   List<Double> outgoingLinksCount = new ArrayList<Double>();
   List<String> links = new ArrayList<String>();
   double sigma=0.1;
   Matrix matrixAdj = null;
   int length=0;

   /**
    * This method computes the page rank scores by using the power iteration method
    * @throws Exception
    */

   public void calculatePageRank() throws Exception
   {
      Connection conn = DBconnection.getCon(Literals.dbcon, true);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT docid FROM documents order by docid ASC");
      while (rs.next()) {
//         System.out.println("inside while");
         docids.add(rs.getInt("docid"));
      }
      length=docids.size();
//      System.out.println("docids: "+ Arrays.toString(docids.toArray()));
      rs = stmt.executeQuery("SELECT from_docid,to_docid FROM links");
      while (rs.next()) {
         String link= Integer.toString(rs.getInt("from_docid"));
         link= link.concat("to");
         link= link.concat(Integer.toString(rs.getInt("to_docid")));
         links.add(link);
      }
//      System.out.println("links: "+ Arrays.toString(links.toArray()));
      matrixAdj = SparseMatrix.zero(length,length);

      // Compute Adjacency Matrix
      matrixAdj= computeAdjacencyMatrix();
//      System.out.println("Outgoing Links: \n"+Arrays.toString(outgoingLinksCount.toArray()));
//      System.out.println("Adjacency Matrix: \n"+ matrixAdj);

      //Compute T Matrix
      matrixAdj= computeT_Matrix();
//      System.out.println("T Matrix: \n"+ matrixAdj);

      //Transition Matrix
      matrixAdj= computeTransitionMatrix();
//      System.out.println("Transition Matrix : \n"+ matrixAdj);

      //power iteration method
      Vector piVal= Vector.zero(length);
      piVal.set(0,1);
      Vector piIter= null;
      System.out.println("initial pi Value : \n"+ piVal);
      while(true)
      {
         piIter=piVal.multiply(matrixAdj);
         double distance = computeEuclideanDistance(piVal,piIter);
         if(distance < 0.0001)
         {
            piVal=piIter;
            System.out.println("pi Value : \n"+ piVal);
            break;
         }
         piVal=piIter;
      }
      updateDocsTb(conn,docids,piVal);
      conn.close();
      stmt.close();
      System.out.println("Page Rank computed");
   }

   /**
    * This method updates the page rank values to the documents table
    * @param conn : contains the connection object
    * @param ids : contains the list of document ids
    * @param piVal :  contains the final page rank score vector
    * @throws Exception
    */

   private void updateDocsTb(Connection conn,List<Integer> ids,Vector piVal) throws Exception
   {
      String querystr = "UPDATE documents set page_rank=? where docid = ?";
      PreparedStatement pstmt = conn.prepareStatement(querystr);
      for(int i=0;i<length;i++)
      {
         pstmt.setDouble(1, piVal.get(i));
         pstmt.setInt(2, ids.get(i));
         pstmt.addBatch();
      }
      pstmt.executeBatch();
   }

   /**
    * This method computes the euclidean distance between the old and new pi vector values
    * @param v : Previous pi value vector
    * @param w : New pi value vector
    * @return : returns the euclidean distance between the two vectors
    */

   private double computeEuclideanDistance(Vector v, Vector w)
   {
      double distance=0;
      for(int i=0;i<length;i++)
      {
         distance += Math.pow((v.get(i)-w.get(i)),2);
      }
      distance=Math.sqrt(distance);
//      System.out.println("distance Value : \n"+ distance);
      return distance;
   }

   /**
    * This method constructs the adjacency matrix based on the web graph built
    * @return : returns the constructed adjacency matrix
    */

   private Matrix computeAdjacencyMatrix()
   {
      // Compute Adjacency Matrix
      for(int i=0;i<length;i++)
      {
         double counter=0;
         for(int j=0;j<length;j++)
         {
            StringBuffer text = new StringBuffer();
            text.append(Integer.toString(docids.get(i)));
            text.append("to");
            text.append(Integer.toString(docids.get(j)));
//            System.out.println("text: "+ text.toString());
            if(links.contains(text.toString())){
               System.out.println("link exists for "+ text.toString());
               matrixAdj.set(i,j,1);
               counter++;
            }
         }
         outgoingLinksCount.add(counter);
      }
      return matrixAdj;
   }

   /**
    * This method computes the T Matrix from the adjacency matrix
    * @return : returns the T Matrix
    */

   private Matrix computeT_Matrix()
   {
      for(int i=0; i<length;i++)
      {
         double count=outgoingLinksCount.get(i);
         Vector row = matrixAdj.getRow(i);
         if(count>0)
            row=row.multiply(1.0/count);
         else
            row=row.add(1.0/length);
         matrixAdj.setRow(i, row);
      }
      return matrixAdj;
   }

   /**
    * This method computes the Transition Matrix
    * @return : returns the Transition Matrix
    */

   private Matrix computeTransitionMatrix()
   {
      Matrix jMatrix = Matrix.constant(1,length,(1.0/length));
      Matrix m2= Matrix.unit(length,1);
      m2=m2.multiply(sigma);
      m2=m2.multiply(jMatrix);
//      System.out.println("Matrix M2: \n"+ m2);
      matrixAdj = matrixAdj.multiply(1.0-sigma).add(m2);
      return matrixAdj;
   }


}
