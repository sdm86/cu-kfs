/**
 * 
 */
package edu.cornell.kfs.module.purap;

import org.kuali.kfs.module.purap.PurapKeyConstants;

/**
 *
 */
public class CUPurapKeyConstants extends PurapKeyConstants {

    public static final String PURAP_ITEM_NONQTY = "error.purap.item.itemtype.nonqty";
    public static final String PURAP_ITEM_NEW_NONQTY = "error.purap.item.new.itemtype.nonqty";
    public static final String PURAP_MOPOT_REQUIRED_DATA_MISSING = "error.purap.mopot.required.data.missing";  //KFSPTS-1458
    
    public static final String I_WANT_ITEMS_SOLE_SOURCE_NOTE = "note.iWant.document.itemSoleSource";
    
    //error messages
    public static final String ERROR_IWNT_CONMPLETE_ORDER_OPTION_REQUIRED = "error.iWant.document.completeOption.required";
    public static final String ERROR_POA_INITIATOR_CANNOT_ADHOC_TO_FO = "error.poa.initiator.cannot.adhoc.to.fo";
    public static final String ERROR_RECEIVING_NOT_ALLOWED_FOR_NON_QTY_PO = "error.receiving.not.allowed.for.noqty.pos";
    public static final String ERROR_RECEIVING_LINE_QTYRECEIVED_GT_QTYORDERED = "errors.receivingLine.quantityReceivedGreaterThanQuantityordered";
    public static final String ERROR_ADD_NEW_RECEIVING_LINE = "errors.add.new.receivingLine";
    public static final String ERROR_ADD_NEW_NOTE_SEND_TO_VENDOR_NO_ATT = "errors.add.new.note.sendToVendor.noAtt";
    public static final String ERROR_EXCEED_SQ_NUMBER_OF_ATT_LIMIT = "errors.exceed.sq.number.of.att.limit";
    public static final String ERROR_REASON_IS_REQUIRED = "errors.reason.is.required";
    public static final String ERROR_ATT_FILE_SIZE_OVER_LIMIT = "errors.att.filesize.over.limit";
   
    public static final String ATTACHMENT_QUESTION_CONFIRM_CHANGE = "attachment.message.confirm.change";
    
    // KFSPTS-2096
    public static final String PURAP_MIX_ITEM_QTY_NONQTY = "error.purap.mix.item.itemtype.nonqty";


}
