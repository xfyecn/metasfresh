import { fetchTab } from './WindowActions';
import { getLayout, getData } from '../api';
import {
  UPDATE_INLINE_TAB_ITEM_FIELDS,
  UPDATE_INLINE_TAB_WRAPPER_FIELDS,
  SET_INLINE_TAB_WRAPPER_DATA,
  SET_INLINE_TAB_LAYOUT_AND_DATA,
  SET_INLINE_TAB_ADD_NEW,
} from '../constants/ActionTypes';

/*
 * @method updateInlineTabItemFields
 * @summary Action creator for updating the fields for the `InlineTab` Item
 *
 * @param {string} inlineTabId
 * @param {string} rowId
 * @param {object} fieldsByName
 */
export function updateInlineTabItemFields({ inlineTabId, fieldsByName }) {
  return {
    type: UPDATE_INLINE_TAB_ITEM_FIELDS,
    payload: { inlineTabId, fieldsByName },
  };
}

/*
 * @method updateInlineTabWrapperFields
 * @summary Action creator for updating the fields for the `InlineTab` Wrapper
 *
 * @param {string} inlineTabWrapperId
 * @param {string} rowId
 * @param {object} fieldsByName
 */
export function updateInlineTabWrapperFields({
  inlineTabWrapperId,
  rowId,
  response,
}) {
  return {
    type: UPDATE_INLINE_TAB_WRAPPER_FIELDS,
    payload: { inlineTabWrapperId, rowId, response },
  };
}

/*
 * Action creator called to set the inlineTabWrapper branch in the redux store with the data payload
 */
export function setInlineTabWrapperData({ inlineTabWrapperId, data }) {
  return {
    type: SET_INLINE_TAB_WRAPPER_DATA,
    payload: { inlineTabWrapperId, data },
  };
}

/*
 * Action creator called to set the inlineTab branch in the redux store with the data payload
 */
export function setInlineTabLayoutAndData({ inlineTabId, data }) {
  return {
    type: SET_INLINE_TAB_LAYOUT_AND_DATA,
    payload: { inlineTabId, data },
  };
}

/*
 * Action creator called to set the inlineTab AddNew form related data in the store
 */
export function setInlineTabAddNew({ visible, windowId, tabId, rowId }) {
  return {
    type: SET_INLINE_TAB_ADD_NEW,
    payload: { visible, windowId, tabId, rowId },
  };
}

/*
 * @method fetchInlineTabWrapperData
 * @summary Action creator for fetching the data for the `InlineTab` Wrapper (note: wrapper not the inline tab item!)
 *
 * @param {string} windowId
 * @param {string} tabId
 * @param {string} docId
 * @param {string} query
 */
export function fetchInlineTabWrapperData({ windowId, tabId, docId, query }) {
  return (dispatch) => {
    dispatch(fetchTab({ tabId, windowId, docId, query })).then((tabData) => {
      dispatch(
        setInlineTabWrapperData({
          inlineTabWrapperId: `${windowId}_${tabId}_${docId}`,
          data: tabData,
        })
      );
    });
  };
}

/*
 * @method getInlineTabLayoutAndData
 * @summary Action creator for fetching and updating the layout and data for the `inlineTab`
 *
 * @param {string} windowId
 * @param {string} tabId
 * @param {string} docId
 * @param {string} rowId
 */
export function getInlineTabLayoutAndData({ windowId, tabId, docId, rowId }) {
  return (dispatch) => {
    getLayout('window', windowId, tabId, null, null, false).then(
      ({ data: layoutData }) => {
        getData({
          entity: 'window',
          docType: windowId,
          docId,
          tabId,
          fetchAdvancedFields: false,
        }).then(({ data: respFields }) => {
          const { result } = respFields;
          const wantedData = result.filter((item) => item.rowId === rowId);
          dispatch(
            setInlineTabLayoutAndData({
              inlineTabId: `${windowId}_${tabId}_${rowId}`,
              data: { layout: layoutData, data: wantedData[0] },
            })
          );
        });
      }
    );
  };
}

export function inlineTabAfterGetLayout({ data, disconnectedData }) {
  return (dispatch) => {
    const inlineTabTargetId = `${disconnectedData.windowId}_${
      disconnectedData.tabId
    }_${disconnectedData.rowId}`;
    dispatch(
      setInlineTabLayoutAndData({
        inlineTabId: inlineTabTargetId,
        data: { layout: data, data: disconnectedData },
      })
    );
    dispatch(
      setInlineTabAddNew({
        visible: true,
        windowId: disconnectedData.windowId,
        tabId: disconnectedData.tabId,
        rowId: disconnectedData.rowId,
      })
    );
  };
}

export function patchInlineTab({ ret, windowId, tabId, docId, rowId }) {
  return (dispatch) => {
    ret.then((response) => {
      dispatch(
        updateInlineTabWrapperFields({
          inlineTabWrapperId: `${windowId}_${tabId}_${docId}`,
          rowId,
          response: response[0],
        })
      );
      dispatch(
        updateInlineTabItemFields({
          inlineTabId: `${windowId}_${tabId}_${rowId}`,
          fieldsByName: response[0].fieldsByName,
        })
      );
    });
  };
}
