import React from 'react';
import nock from 'nock';
import { shallow, mount } from 'enzyme';
import configureStore from 'redux-mock-store';
import merge from 'merge';
import viewHandler from '../../../reducers/viewHandler';
import InlineTabWrapper from '../../../components/widget/InlineTabWrapper';
import hotkeys from '../../../../test_setup/fixtures/hotkeys.json';
import keymap from '../../../../test_setup/fixtures/keymap.json';
import { ShortcutProvider } from '../../../components/keyshortcuts/ShortcutProvider';
import { initialState as appHandlerState } from '../../../reducers/appHandler';
import { initialState as windowHandlerState } from '../../../reducers/windowHandler';
import tablesHandler from '../../../reducers/tables';
import { Provider } from 'react-redux';
import props from '../../../../test_setup/fixtures/widget/inline_tab_wrapper.json';
import tabData from '../../../../test_setup/fixtures/widget/inline_tab_data.json';
import inlineTabStoreMore from '../../../../test_setup/fixtures/widget/inline_tab_data_more.json';
import inlineTabStore from '../../../../test_setup/fixtures/widget/inlineTabStore.json';
import thunk from 'redux-thunk';
const middlewares = [thunk];

const mockStore = configureStore(middlewares);

const createStore = function(state = {}) {
  const res = merge.recursive(
    true,
    {
      appHandler: {
        ...appHandlerState,
        me: { timeZone: 'America/Los_Angeles' },
      },
      windowHandler: { ...windowHandlerState },
      ...viewHandler,
      tables: { ...tablesHandler(undefined, {}) },
    },
    state
  );

  return res;
};

describe('InlineTabWrapper component', () => {
  describe('rendering tests:', () => {
    it('renders without errors', () => {
      shallow(<InlineTabWrapper {...props} />);
    });

    it('renders a line properly', () => {
      const initialState = createStore({
        windowHandler: {
          allowShortcut: true,
          modal: {
            visible: true,
          },
          inlineTab: inlineTabStore,
        },
      });
      const store = mockStore(initialState);

      nock(config.API_URL)
        .defaultReplyHeaders({ 'access-control-allow-origin': '*' })
        .get(`/window/123/2156425/AD_Tab-222/`)
        .reply(200, tabData);

      const wrapper = mount(
        <ShortcutProvider hotkeys={hotkeys} keymap={keymap}>
          <Provider store={store}>
            <InlineTabWrapper {...props} />
          </Provider>
        </ShortcutProvider>
      );
      const htmlOutput = wrapper.html();
      expect(htmlOutput).toContain('<span>Testadresse 3</span');
      expect(htmlOutput).toContain('inlinetab-action-button');
      expect(htmlOutput).not.toContain('meta-icon-fullscreen');
    });
  });

  it('renders more line properly', () => {
    props.dataId = '2155894'; // we pass different docId such that will match the selector used for the mocked up data
    const initialState = createStore({
      windowHandler: {
        allowShortcut: true,
        modal: {
          visible: true,
        },
        inlineTab: inlineTabStoreMore,
      },
    });
    const store = mockStore(initialState);

    nock(config.API_URL)
      .defaultReplyHeaders({ 'access-control-allow-origin': '*' })
      .get(`/window/123/2155894/AD_Tab-222/`)
      .reply(200, tabData);

    const wrapper = mount(
      <ShortcutProvider hotkeys={hotkeys} keymap={keymap}>
        <Provider store={store}>
          <InlineTabWrapper {...props} />
        </Provider>
      </ShortcutProvider>
    );
    const htmlOutput = wrapper.html();

    expect(htmlOutput).toContain('Amerikanische Jungferninseln');
    expect(htmlOutput).toContain('Amerikanisch-Samoa');
    expect(htmlOutput).toContain('Amerikanisch-Samoa (2)');
    expect(htmlOutput).toContain('Am Nossbacher Weg 2');
    expect(htmlOutput).toContain(
      'Antarktis (Sonderstatus durch Antarktis-Vertrag)'
    );
    expect(htmlOutput).not.toContain('Russland'); // only five rows shown - this should not be rendered
    expect(htmlOutput).toContain('meta-icon-fullscreen');
  });
});
