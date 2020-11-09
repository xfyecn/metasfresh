import React, { PureComponent } from 'react';
import { noop } from 'lodash';

import { cancelablePromise } from '../../utils';

const delay = (n) => new Promise((resolve) => setTimeout(resolve, n));

const skipClickOnDoubleClick = (WrappedComponent) => {
  class ComponentWrapper extends PureComponent {
    componentWillUnmount() {
      // cancel all pending promises to avoid
      // side effects when the component is unmounted
      this.clearPendingPromises();
    }

    pendingPromises = [];

    appendPendingPromise = (promise) =>
      (this.pendingPromises = [...this.pendingPromises, promise]);

    removePendingPromise = (promise) =>
      (this.pendingPromises = this.pendingPromises.filter(
        (p) => p !== promise
      ));

    clearPendingPromises = () => this.pendingPromises.map((p) => p.cancel());

    handleClick = (e, item) => {
      e.persist();
      // create the cancelable promise and add it to
      // the pending promises queue
      const waitForClick = cancelablePromise(delay(300));
      this.appendPendingPromise(waitForClick);

      return waitForClick.promise
        .then(() => {
          // if the promise wasn't cancelled, we execute
          // the callback and remove it from the queue
          this.removePendingPromise(waitForClick);
          this.props.onClick(e, item);
        })
        .catch((errorInfo) => {
          // console.log('ERROR: ', errorInfo)

          // rethrow the error if the promise wasn't
          // rejected because of a cancelation
          this.removePendingPromise(waitForClick);
          if (!errorInfo.isCanceled) {
            throw errorInfo.error;
          }
        });
    };

    handleDoubleClick = (id) => {
      // all (click) pending promises are part of a
      // dblclick event so we cancel them
      this.clearPendingPromises();
      this.props.onDoubleClick(id);
    };

    render() {
      return (
        <WrappedComponent
          {...this.props}
          onClick={this.handleClick}
          onDoubleClick={this.handleDoubleClick}
        />
      );
    }
  }

  ComponentWrapper.displayName = `withClickPrevention(${WrappedComponent.displayName ||
    WrappedComponent.name ||
    'Component'})`;

  ComponentWrapper.defaultProps = {
    onClick: noop,
    onDoubleClick: noop,
  };

  return ComponentWrapper;
};

export default skipClickOnDoubleClick;
