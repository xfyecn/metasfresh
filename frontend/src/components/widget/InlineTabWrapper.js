/**
 *  InlineTabWrapper - component
 *
 *  This is the component that will render when a widget of type 'inlineTab' is present in the layout
 *  - it contains the rows of the inlineTab plus the form that would allow addition of new entries (rows)
 *  - rows are rendered using the <InlineTab> component
 *  - the form for adding a new row is rendered by the SubSections
 *    + the functionality is similar to Add New under the tabs
 *    + the difference is that the  data is disconnected from the main structure
 *    + the data is stored in the Redux Store under the windowHandler.inlineTab.wrapperData path
 *  - actions are found in: src/action/InlineTabActions
 *  - wrapperData index is formed using ${windowId}_${tabId}_${docId} pattern
 *  - adding a new record calls the createWindow action with param NEW for row. From here you will get the rowId on the newly created entry
 *  - data sync with the redux store is done via fetchInlineTabWrapperData action
 *  - inlineTab items keys are formed using ${windowId}_${tabId}_${rowId} pattern
 */
import React, { PureComponent } from 'react';
import InlineTab from './InlineTab';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { createWindow } from '../../actions/WindowActions';
import {
  fetchInlineTabWrapperData,
  setInlineTabAddNew,
  setInlineTabShowMore,
} from '../../actions/InlineTabActions';
import SectionGroup from '../SectionGroup';
import counterpart from 'counterpart';
import classnames from 'classnames';
import { INLINE_TAB_SHOW_MORE_FROM } from '../../constants/Constants';

class InlineTabWrapper extends PureComponent {
  constructor(props) {
    super(props);
    this.updateTable(); // this is getting table rows
  }

  /**
   * @method updateTable
   * @summary does a refresh of the table data
   */
  updateTable = (postDeletion = false) => {
    const query = '';
    const {
      inlineTab: { windowId, tabId },
      dataId: docId,
      fetchInlineTabWrapperData,
      rowId,
    } = this.props;
    fetchInlineTabWrapperData({
      tabId,
      windowId,
      docId,
      query,
      rowId,
      postDeletion,
    });
  };

  /**
   * @method showAddNewForm
   * @summary creates a new Window, the data from here are used for rendering the SectionGroup and also from here we get the rowId
   *          a thing to note here is the `disconnected` flag which specifies the type of Widget, this serves the logic in the WindowActions
   */
  showAddNewForm = () => {
    const {
      createWindow,
      inlineTab: { windowId, tabId },
      dataId: docId,
    } = this.props;
    createWindow({
      windowId,
      docId,
      tabId,
      rowId: 'NEW',
      isModal: true,
      isAdvanced: false,
      disconnected: 'inlineTab',
    });
  };

  /**
   * @method handleFormClose
   * @summary - actions triggered on closing the form, here we toggle the form visibility using the setInlineTabAddNew action
   */
  handleFormClose = () => {
    const {
      setInlineTabAddNew,
      inlineTab: { windowId, tabId },
      dataId: docId,
      rowId,
    } = this.props;
    setInlineTabAddNew({ visible: false, windowId, docId, tabId, rowId });
    this.updateTable();
  };

  /**
   * @method toggleShowMore
   * @summary - sets the showMore flag in the redux store path windowHanlder.inlineTab.showMore[ID] to `false/true` value
   *            using the setInlineTabShowMore action
   */
  toggleShowMore = () => {
    const {
      inlineTab: { windowId, tabId },
      dataId: docId,
      setInlineTabShowMore,
      showMore,
    } = this.props;
    setInlineTabShowMore({
      inlineTabWrapperId: `${windowId}_${tabId}_${docId}`,
      showMore: !showMore,
    });
  };

  /**
   * @method getFieldsDisplayOrder
   * @summanry - function used to get the order from the `elements` array of the `inlineTab`
   *             currently we are getting the first four. In future development this might be refactored for
   *             more specific ordering. This is the reason I did not even put that in a constant.
   * @param {array} inlineTab
   */
  getFieldsDisplayOrder = (inlineTab) => {
    const { elements } = inlineTab;
    const orderFields = [];
    elements.map((elementItem, index) => {
      if (index < 4) orderFields.push(elementItem.fields[0].field);
      return elementItem;
    });
    return orderFields;
  };

  render() {
    const {
      tabData,
      addNewFormVisible,
      addNewData,
      rowId,
      inlineTab: { caption, tabId },
      dataId,
      showMore,
      inlineTab,
    } = this.props;

    if (!tabData) return false;

    const inlineFieldsDisplayOrder = this.getFieldsDisplayOrder(inlineTab);

    return (
      <div
        className={classnames('inline-tab-wrapper', {
          'tabs-fullscreen container-fluid inline-tab-fullscreen-top-offset':
            !showMore &&
            !addNewFormVisible &&
            tabData.length > INLINE_TAB_SHOW_MORE_FROM,
        })}
      >
        <span className="main-label">{caption}</span>
        {/* `Show less` - button */}
        {!showMore &&
          !addNewFormVisible &&
          tabData.length > INLINE_TAB_SHOW_MORE_FROM && (
            <div className="inlinetab-showmore bottom-offset">
              <button
                className="btn-icon btn-meta-outline-secondary pointer btn-fullscreen"
                onClick={this.toggleShowMore}
              >
                <i className="meta-icon-fullscreen" />
              </button>
            </div>
          )}

        {/* InlineTab Row Items */}
        {tabData &&
          tabData.map((tabItem, index) => {
            if (showMore && index >= INLINE_TAB_SHOW_MORE_FROM) {
              return false;
            } else {
              return tabItem ? (
                <InlineTab
                  key={`${index}_${tabItem.rowId}`}
                  fieldsOrder={inlineFieldsDisplayOrder}
                  updateTable={this.updateTable}
                  {...tabItem}
                />
              ) : (
                false
              );
            }
          })}
        <div className="clearfix" />
        {/* Add content wrapper */}
        <div>
          <div>
            {/* `Add New` - button */}
            {!addNewFormVisible && (
              <div className="inlinetab-action-button">
                <button
                  className="btn btn-meta-outline-secondary btn-distance btn-sm"
                  onClick={this.showAddNewForm}
                >
                  {counterpart.translate('window.addNew.caption')}
                </button>
                <div className="clearfix" />
              </div>
            )}
            {/* `Show more...` - button */}
            {showMore && !addNewFormVisible && (
              <div className="inlinetab-showmore">
                <button
                  className="btn-icon btn-meta-outline-secondary pointer btn-fullscreen"
                  onClick={this.toggleShowMore}
                >
                  <i className="meta-icon-fullscreen" />
                </button>
              </div>
            )}
          </div>
          {/* Actual form */}
          {addNewFormVisible && rowId && addNewData && (
            <div className="inline-tab-active">
              <div className="inline-tab-content">
                <div>
                  <div className="inlinetab-form-header">
                    {counterpart.translate('window.addNew.caption')}
                  </div>
                  <div className="inlinetab-close">
                    <i
                      className="meta-icon-close-alt"
                      onClick={this.handleFormClose}
                    />
                  </div>
                </div>
                <div className="clearfix" />
                <div className="inline-tab-separator" />
                {/* Actual content */}
                <SectionGroup
                  data={addNewData.data}
                  dataId={dataId}
                  layout={addNewData.layout}
                  modal={true}
                  tabId={tabId}
                  rowId={rowId}
                  isModal={true}
                  tabsInfo={null}
                  disconnected={`inlineTab`} // This has to match the windowHandler.inlineTab path in the redux store
                />
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }
}

InlineTabWrapper.propTypes = {
  caption: PropTypes.string.isRequired,
  inlineTab: PropTypes.object.isRequired,
  dataId: PropTypes.string.isRequired,
  createWindow: PropTypes.func.isRequired,
  widgetData: PropTypes.array,
  fetchInlineTabWrapperData: PropTypes.func.isRequired,
  tabData: PropTypes.array,
  rowId: PropTypes.any,
  addNewData: PropTypes.any,
  addNewFormVisible: PropTypes.bool,
  setInlineTabAddNew: PropTypes.func.isRequired,
  showMore: PropTypes.bool,
  setInlineTabShowMore: PropTypes.func.isRequired,
};

/**
 * @summary - here we do the actual mapping of the redux store props to the local props we need for rendering the elements
 * @param {*} state - redux state
 * @param {*} props - local props
 */
const mapStateToProps = (state, props) => {
  const {
    inlineTab: { windowId, tabId },
    dataId: docId,
  } = props;

  const {
    windowHandler: { inlineTab },
  } = state;

  const selector = `${windowId}_${tabId}_${docId}`;
  const tabData = inlineTab.wrapperData[selector]
    ? inlineTab.wrapperData[selector]
    : null;
  const showMore = inlineTab.showMore[selector]
    ? inlineTab.showMore[selector]
    : false;

  const addNew = inlineTab.addNew[selector] ? inlineTab.addNew[selector] : null;
  const addNewFormVisible = addNew ? addNew.visible : false;
  const rowId = addNew ? addNew.rowId : undefined;
  const addNewData = inlineTab[`${windowId}_${tabId}_${rowId}`];

  return {
    tabData,
    addNewFormVisible,
    addNewData,
    rowId,
    showMore,
  };
};

export default connect(
  mapStateToProps,
  {
    fetchInlineTabWrapperData,
    createWindow,
    setInlineTabAddNew,
    setInlineTabShowMore,
  }
)(InlineTabWrapper);
