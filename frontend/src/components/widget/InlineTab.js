import React, { PureComponent } from 'react';
import classnames from 'classnames';
import PropTypes from 'prop-types';
import Table from '../../containers/Table';
import { getLayout, getData } from '../../api/view';

class InlineTab extends PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: true,
      layout: null,
      data: null,
    };
  }

  toggleOpen = () => {
    const { windowId, id: docId, tabId } = this.props;
    this.setState(
      (prevState) => {
        return { isOpen: !prevState.isOpen };
      },
      () => {
        this.state.isOpen &&
          getLayout('window', windowId, tabId, null, null, false).then(
            ({ data: layoutData }) => {
              getData({
                entity: 'window',
                docType: windowId,
                docId,
                fetchAdvancedFields: false,
              }).then(({ fieldsByName }) =>
                this.setState({ layout: layoutData, data: fieldsByName })
              );
            }
          );
        !this.state.isOpen && this.setState({ layout: null });
      }
    );
  };

  render() {
    const { widgetData, windowId, dataId, onRefreshTable } = this.props;
    const { isOpen } = this.state;

    const {
      tabId,
      caption,
      description,
      internalName,
      queryOnActivate,
      supportQuickInput,
      defaultOrderBys,
      orderBy,
    } = widgetData;

    return (
      <div>
        <div
          className={classnames(
            { 'inline-tab': !isOpen },
            { 'inline-tab-active': isOpen },
            { 'form-control-label': true }
          )}
          onClick={this.toggleOpen}
        >
          <div className="pull-left">
            <span className="arrow-pointer" />
          </div>
          {/* Header  */}
{/*          <div className="pull-left offset-left">
            <span>{fieldsByName.Name.value}</span>&nbsp;&nbsp;
            <span>{fieldsByName.Address.value}</span>
          </div>*/}

          {/* Content */}
          {isOpen && (
            <div className="inline-tab-content">
              <div className="inline-tab-separator" />
              <Table
                {...{
                  caption,
                  description,
                  tabId,
                  windowId,
                  internalName,
                }}
                entity="window"
                key={tabId}
                orderBy={orderBy || defaultOrderBys}
                docId={dataId}
                queryOnActivate={queryOnActivate}
                supportQuickInput={supportQuickInput}
                updateDocList={onRefreshTable}
              />
            </div>
          )}
        </div>
      </div>
    );
  }
}

InlineTab.propTypes = {
  windowId: PropTypes.string.isRequired,
  id: PropTypes.string.isRequired,
  rowId: PropTypes.string.isRequired,
  tabId: PropTypes.string.isRequired,
  fieldsByName: PropTypes.object,
};

export default InlineTab;
